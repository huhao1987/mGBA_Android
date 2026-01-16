# mGBA CodeBreaker Cheat Codes Reference (中英对照进阶版)

This document details all supported **CodeBreaker (GBA)** cheat code formats in mGBA, including advanced multi-line codes.
本文档详细列出了 mGBA 支持的所有 **CodeBreaker (GBA)** 金手指指令格式，包含高级多行代码。

## 1. Basic Memory Writes / 基础内存写入
*(Most Common / 最常用)*

| OpCode (指令) | Format (格式) | Description (说明) |
| :--- | :--- | :--- |
| **3** | `3AAAAAAA 00VV` | **8-bit Write** (8位写入)<br>Writes byte `VV` (00-FF) to address `AAAAAAA`.<br>向地址 `AAAAAAA` 写入数值 `VV`。<br>*Best for: Items quantity, Flags.* (适合：物品数量、图鉴开关) |
| **8** | `8AAAAAAA VVVV` | **16-bit Write** (16位写入)<br>Writes word `VVVV` (0000-FFFF) to address `AAAAAAA`.<br>向地址 `AAAAAAA` 写入数值 `VVVV`。<br>*Best for: Money, Stats.* (适合：金钱、能力值) |

## 2. Advanced Block Writes / 高级批量写入
*(For modifying large areas of memory / 用于修改大片内存)*

### Type 4: Slide Code (等差数列写入)
Writes a sequence of values with a pattern.
按规律自动写入一系列数值。

**Format:**
```
Line 1: 4AAAAAAA VVVV  (Base Address, Base Value / 起始地址, 起始数值)
Line 2: IIIICCCC SSSS  (Increments, Count, Step / 增量, 次数, 步长)
```
*   `AAAAAAA`: Start Address (起始地址)
*   `VVVV`: Start Value (起始数值)
*   `IIII`: Value Increment (每次写入后数值增加多少)
*   `CCCC`: Repeat Count (重复写入多少次)
*   `SSSS`: Address Step (每次写入后地址增加多少，通常是 `0002`)

**Example (全道具99个):**
```
42005000 0063   // Write 99 (0x63) to 02005000 (First Item Qty)
00000064 0004   // Val Inc=0, Count=100 (0x64), Addr Step=4 (Next Item Qty)
```

### Type 5: Fill List (列表填充)
Writes a raw list of data sequentially.
依次写入一串自定义数据。

**Format:**
```
Line 1: 5AAAAAAA NNNN  (Start Address, Count / 起始地址, 数据个数)
Line 2: D1D1D2D2 D3D3  (Data 1, Data 2, Data 3 / 数据1, 数据2, 数据3)
Line 3: D4D4D5D5 D6D6  ...
```
*   Writes `NNNN` words (16-bit) starting at `AAAAAAA`.
*   Each subsequent line provides 3 words of data.
*   从起始地址开始，连续写入 NNNN 个 16位数据。

## 3. Conditional Execution / 条件执行
*(Execute next line ONLY if condition is met / 仅当条件满足时执行下一行)*

| OpCode (指令) | Format (格式) | Description (说明) |
| :--- | :--- | :--- |
| **7** | `7AAAAAAA VVVV` | **IF EQUAL** (如果相等 ==) |
| **A** | `AAAAAAAA VVVV` | **IF NOT EQUAL** (如果不等 !=) |
| **B** | `BAAAAAAA VVVV` | **IF GREATER** (如果大于 >) |
| **C** | `CAAAAAAA VVVV` | **IF LESS** (如果小于 <) |
| **D** | `D0000020 VVVV` | **IF KEY PRESSED** (按键判断)<br>Execute next line if key `VVVV` is pressed.<br>当按下键 `VVVV` 时，执行下一行。<br>*Key Values (键值):* <br>`0001`=A, `0002`=B, `0004`=Select, `0008`=Start<br>`0010`=Right, `0020`=Left, `0040`=Up, `0080`=Down<br>`0100`=R, `0200`=L |
| **F** | `FAAAAAAA VVVV` | **IF AND** (逻辑与判断)<br>Execute next line if `(Value & VVVV)` is non-zero. |

## 4. Mathematical Operations / 数学运算

| OpCode (指令) | Format (格式) | Description (说明) |
| :--- | :--- | :--- |
| **E** | `EAAAAAAA VVVV` | **ADD** (加法)<br>Adds `VVVV` to value at `AAAAAAA`. |
| **2** | `2AAAAAAA VVVV` | **OR** (逻辑或 \|) |
| **6** | `6AAAAAAA VVVV` | **AND** (逻辑与 &) |

## 5. System Codes / 系统指令

| OpCode (指令) | Format (格式) | Description (说明) |
| :--- | :--- | :--- |
| **0** | `0000AAAA 000X` | **Game ID** (游戏码) - Ignored by mGBA |
| **1** | `1AAAAAAA VVVV` | **Master Code** (必须码) - Optional in mGBA |
| **9** | `9AAAAAAA VVVV` | **Encryption Seed** (加密种子) |

---

# GameShark / Action Replay Section (GameShark 补充说明)

GameShark (v1/v2) and Action Replay (v3) are also supported but differ significantly from CodeBreaker.
GameShark (v1/v2) 和 Action Replay (v3) 也受支持，但与 CodeBreaker 有明显区别。

### Key Differences (主要区别)
| Feature | CodeBreaker (CB) | GameShark (GS) / Action Replay (AR) |
| :--- | :--- | :--- |
| **Format (格式)** | Clear Opcode + Address.<br>指令+地址清晰可见。 | Packed Bitfields (Complex).<br>指令压缩在位段中，难以肉眼识别。 |
| **Encryption (加密)** | Often Unencrypted (Raw).<br>通常是未加密的。 | **Heavily Encrypted**.<br>几乎总是加密的，需解密才能生效。 |
| **Simplicity (易用)** | High. `3` is byte, `8` is word.<br>简单直观。 | Low. Different versions have different formats.<br>版本混乱 (v1, v2, v3 互不通用)。 |
| **mGBA Support** | **Excellent (Recommended).**<br>完美支持 (推荐使用)。 | Good, but encryption can cause issues.<br>支持，但加密可能导致识别错误。 |

### Common GameShark OpCodes (Old v1/v2)
*   **01/11**: 16-bit Write (`01VVVVAA AAAAAA` - Note address is split!)
*   **0x/2x**: 8-bit Write.

**Recommendation (建议):**
Always try to use **CodeBreaker** codes for GBA emulation. They are more standardized and less prone to broken encryption.
做 GBA 模拟器金手指时，**强烈建议优先使用 CodeBreaker 格式**。
