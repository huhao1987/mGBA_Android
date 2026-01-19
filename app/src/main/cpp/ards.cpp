#include "ards.h"
#include <mgba/core/core.h>
#include <android/log.h>

#define ARDS_TAG "mGBA_ARDS"
#define LOG_ARDS(...) __android_log_print(ANDROID_LOG_DEBUG, ARDS_TAG, __VA_ARGS__)

std::vector<ARDSCode> g_ardsCheats;
ARDSState g_ardsState;

void ARDS_Reset() {
    g_ardsCheats.clear();
    g_ardsState.reset();
    LOG_ARDS("Engine Reset");
}

uint32_t getCodeType(uint32_t op);

void ARDS_AddCheat(uint32_t op, uint32_t val) {
    g_ardsCheats.push_back({op, val});
    LOG_ARDS("Added Code: %08X %08X", op, val);
}

// Helper: Check if we should execute current code based on condition stack
bool ARDS_ShouldExecute() {
    if (!g_ardsState.executionEnabled) return false;
    for (bool cond : g_ardsState.conditionStack) {
        if (!cond) return false;
    }
    return true;
}

// Helper: Memory Access with Offset
uint32_t ARDS_GetFinalAddr(uint32_t addr) {
    return (addr & 0x0FFFFFFF) + g_ardsState.offset;
}

// Helper: Read/Write wrappers for GBA Bus
uint32_t BusRead32(struct mCore* core, uint32_t addr) {
    return core->busRead32(core, addr);
}
uint16_t BusRead16(struct mCore* core, uint32_t addr) {
    return core->busRead16(core, addr);
}
uint8_t BusRead8(struct mCore* core, uint32_t addr) {
    return core->busRead8(core, addr);
}

void BusWrite32(struct mCore* core, uint32_t addr, uint32_t val) {
    core->busWrite32(core, addr, val);
}
void BusWrite16(struct mCore* core, uint32_t addr, uint16_t val) {
    core->busWrite16(core, addr, val);
}
void BusWrite8(struct mCore* core, uint32_t addr, uint8_t val) {
    core->busWrite8(core, addr, val);
}


void ARDS_Run(struct mCore* core) {
    if (g_ardsCheats.empty() || !core) return;

    // Reset state each frame? AR DS specs usually keep state per frame or across frames?
    // "Offset" and "Stash" typically persist until changed.
    // "If" stack usually resets per cheat list execution.
    g_ardsState.conditionStack.clear(); 
    g_ardsState.offset = 0; 
    g_ardsState.stash = 0;
    // Note: Some docs say Stash persists. For safety in GBA context, let's reset to avoid leakage.
    // Actually, user wants Stash to copy data. If we reset, D9 then D6 must be in same frame. Correct.

    for (const auto& code : g_ardsCheats) {
        uint32_t opcode = code.op >> 28; // Top 4 bits
        uint32_t rawAddr = code.op & 0x0FFFFFFF;
        uint32_t val = code.val;
        
        // Always process "D0" (End If) and "E" (Patch) regardless of condition?
        // Standard says: If condition false, skip until D0.
        // So we must check opcode.
        
        bool isExecute = ARDS_ShouldExecute();

        // Handle D0 (End If) specially to pop stack
        if (opcode == 0xD && rawAddr == 0) { // D0000000 00000000 = End IF
             if (!g_ardsState.conditionStack.empty()) {
                 g_ardsState.conditionStack.pop_back();
             }
             continue;
        }
        
        // If inside a FALSE condition, skip everything except D0 (already handled)
        if (!isExecute) continue;

        uint32_t finalAddr = ARDS_GetFinalAddr(rawAddr);

        switch (opcode) {
            case 0x0: // 32-bit Write: 0XXXXXXX YYYYYYYY
                BusWrite32(core, finalAddr, val);
                break;
                
            case 0x1: // 16-bit Write: 1XXXXXXX 0000YYYY
                BusWrite16(core, finalAddr, (uint16_t)val);
                break;
                
            case 0x2: // 8-bit Write: 2XXXXXXX 000000YY
                BusWrite8(core, finalAddr, (uint8_t)val);
                break;

            // 3x - 6x: 32-bit Conditional checks
            case 0x3: // If < (32-bit)
                g_ardsState.conditionStack.push_back(BusRead32(core, finalAddr) < val);
                break;
            case 0x4: // If > (32-bit)
                g_ardsState.conditionStack.push_back(BusRead32(core, finalAddr) > val);
                break;
            case 0x5: // If == (32-bit)
                g_ardsState.conditionStack.push_back(BusRead32(core, finalAddr) == val);
                break;
            case 0x6: // If != (32-bit)
                g_ardsState.conditionStack.push_back(BusRead32(core, finalAddr) != val);
                break;

            // 7x - Ax: 16-bit Conditional checks
            case 0x7: // If < (16-bit)
                g_ardsState.conditionStack.push_back((BusRead16(core, finalAddr) & 0xFFFF) < (val & 0xFFFF));
                break;
            case 0x8: // If > (16-bit)
                g_ardsState.conditionStack.push_back((BusRead16(core, finalAddr) & 0xFFFF) > (val & 0xFFFF));
                break;
            case 0x9: // If == (16-bit)
                g_ardsState.conditionStack.push_back((BusRead16(core, finalAddr) & 0xFFFF) == (val & 0xFFFF));
                break;
            case 0xA: // If != (16-bit)
                g_ardsState.conditionStack.push_back((BusRead16(core, finalAddr) & 0xFFFF) != (val & 0xFFFF));
                break;

            // Bx: Offset Loading
            case 0xB: // Load Offset: BXXXXXXX 00000000
                g_ardsState.offset = BusRead32(core, rawAddr); // No offset applied to pointer read
                break;

            // Dx: Data/Stash Operations
            case 0xD:
                switch (getCodeType(code.op)) { // Custom helper needed or switch on upper bits
                    case 0xD0000000: // End If
                         if (!g_ardsState.conditionStack.empty()) {
                             g_ardsState.conditionStack.pop_back();
                         }
                         break;

                    case 0xD2000000: // Full Terminator
                         g_ardsState.conditionStack.clear();
                         g_ardsState.offset = 0;
                         g_ardsState.stash = 0;
                         g_ardsState.executionEnabled = true;
                         break;
                    
                    case 0xD3000000: // Set Offset
                        g_ardsState.offset = val;
                        break;
                        
                    case 0xD4000000: // Add to Stash: D4000000 XXXXXXXX
                        g_ardsState.stash += val;
                        break;
                        
                    case 0xD5000000: // Set Stash: D5000000 XXXXXXXX
                        g_ardsState.stash = val;
                        break;
                        
                    case 0xD6000000: // Store Stash (32-bit): D6000000 XXXXXXXX
                        BusWrite32(core, ARDS_GetFinalAddr(val), g_ardsState.stash);
                        // Also usually increments offset by 4 in AR DS?
                        g_ardsState.offset += 4; 
                        break;

                    case 0xD7000000: // Store Stash (16-bit)
                         {
                             uint32_t addr = ARDS_GetFinalAddr(val);
                             uint16_t v = (uint16_t)g_ardsState.stash;
                             BusWrite16(core, addr, v);
                             // LOG_ARDS("OP D7: Stored %04X to [%08X]", v, addr);
                             g_ardsState.offset += 2;
                         }
                         break;

                    case 0xD8000000: // Store Stash (8-bit)
                         BusWrite8(core, ARDS_GetFinalAddr(val), (uint8_t)g_ardsState.stash);
                         g_ardsState.offset += 1;
                         break;
                        
                    case 0xD9000000: // Load Stash (32-bit): D9000000 XXXXXXXX
                        g_ardsState.stash = BusRead32(core, ARDS_GetFinalAddr(val));
                        // LOG_ARDS("OP D9: Loaded %08X from [%08X]", g_ardsState.stash, val);
                        break;

                    case 0xDA000000: // Load Stash (16-bit)
                        g_ardsState.stash = BusRead16(core, ARDS_GetFinalAddr(val));
                         // LOG_ARDS("OP DA: Loaded %04X from [%08X]", g_ardsState.stash, val);
                        break;

                    case 0xDB000000: // Load Stash (8-bit)
                        g_ardsState.stash = BusRead8(core, ARDS_GetFinalAddr(val));
                        break;
                        
                    case 0xDC000000: // Add Stash (Offset)
                        g_ardsState.offset += val;
                        break;
                }
                break;
                
             // E: Patch Code (Not implemented fully for brevity, can be added)
             // F: Copy Memory (Not implemented fully)
        }
    }
}

// Helper to mask opcode for Dx switch
uint32_t getCodeType(uint32_t op) {
    return op & 0xFF000000;
}
