#ifndef ARDS_H
#define ARDS_H

#include <stdint.h>
#include <vector>

// Forward declaration of mCore
struct mCore;

// Action Replay DS State
struct ARDSState {
    uint32_t offset;            // Offset Register (D3)
    uint32_t stash;             // Stash Register (Data Register) (D9/D6/D4...)
    
    // Condition Stack for nested IFs
    // Standard AR DS supports limited nesting, but a stack is easiest.
    // Each entry is boolean: true = execute, false = skip.
    std::vector<bool> conditionStack; 
    
    bool executionEnabled;      // Global master switch for the current block
    
    // Loop support could be added here (D1/D2)
    
    ARDSState() {
        reset();
    }
    
    void reset() {
        offset = 0;
        stash = 0;
        conditionStack.clear();
        executionEnabled = true;
    }
};

// Represents a single 64-bit Cheat Code (XXXXXXXX YYYYYYYY)
struct ARDSCode {
    uint32_t op;   // Left part
    uint32_t val;  // Right part
};

// Global container for active cheats
extern std::vector<ARDSCode> g_ardsCheats;
extern ARDSState g_ardsState;

// Functions
void ARDS_Reset();
void ARDS_AddCheat(uint32_t op, uint32_t val);
void ARDS_Run(struct mCore* core);

#endif // ARDS_H
