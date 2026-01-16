package hh.game.mgba_android.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hh.game.mgba_android.R
import hh.game.mgba_android.activity.GameActivity

class HexEditorFragment : DialogFragment() {

    private lateinit var regionSpinner: Spinner
    private lateinit var typeSpinner: Spinner
    private lateinit var rgBase: RadioGroup
    private lateinit var rbHex: RadioButton
    private lateinit var rbDec: RadioButton
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var jumpButton: Button
    private lateinit var hexRecyclerView: RecyclerView
    private lateinit var hexAdapter: HexListAdapter

    private var currentBaseAddress: Int = 0x02000000
    private var currentSize: Int = 0x40000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? GameActivity)?.ResumeGame()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hex_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        regionSpinner = view.findViewById(R.id.region_spinner)
        typeSpinner = view.findViewById(R.id.type_spinner)
        rgBase = view.findViewById(R.id.rg_base)
        rbHex = view.findViewById(R.id.rb_hex)
        rbDec = view.findViewById(R.id.rb_dec)
        searchInput = view.findViewById(R.id.search_input)
        searchButton = view.findViewById(R.id.btn_search)
        jumpButton = view.findViewById(R.id.btn_jump)
        hexRecyclerView = view.findViewById(R.id.hex_recycler_view)

        setupRegionSpinner()
        setupTypeSpinner()
        setupRecyclerView()
        setupSearch()
        setupJump()
    }

    private fun setupRegionSpinner() {
        val regions = listOf(
            "WRAM (02000000)" to Pair(0x02000000, 0x40000),
            "IRAM (03000000)" to Pair(0x03000000, 0x8000),
            "BIOS (00000000)" to Pair(0x00000000, 0x4000),
            "ROM  (08000000)" to Pair(0x08000000, 0x1000000)
        )
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, regions.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = adapter

        regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val region = regions[position].second
                currentBaseAddress = region.first
                currentSize = region.second
                hexAdapter.updateRegion(currentBaseAddress, currentSize)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTypeSpinner() {
        // Map display name to byte size (1, 2, 4)
        val types = listOf(
            "Byte (8-bit)" to 1,
            "Short (16-bit)" to 2,
            "Int (32-bit)" to 4
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
    }

    private fun setupRecyclerView() {
        hexRecyclerView.layoutManager = LinearLayoutManager(context)
        hexAdapter = HexListAdapter(activity as GameActivity, currentBaseAddress, currentSize)
        hexRecyclerView.adapter = hexAdapter
    }

    private fun setupSearch() {
        searchButton.setOnClickListener {
            val text = searchInput.text.toString()
            if (text.isBlank()) return@setOnClickListener

            val isHex = rbHex.isChecked
            val size = when (typeSpinner.selectedItemPosition) {
                0 -> 1 // Byte
                1 -> 2 // Short
                2 -> 4 // Int
                else -> 4
            }

            val value = try {
                if (isHex) {
                     // Remove 0x prefix if present
                     val cleanText = text.replace("0x", "", ignoreCase = true)
                     cleanText.toInt(16)
                } else {
                     text.toInt()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid Value Format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val activity = activity as GameActivity
            val results = activity.nativeMemorySearch(value, size)
            
            if (results != null && results.isNotEmpty()) {
                showSearchResults(results)
            } else {
                Toast.makeText(context, "No matches found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupJump() {
        jumpButton.setOnClickListener {
            val input = EditText(context)
            input.hint = "Address (Hex)"
            AlertDialog.Builder(requireContext())
                .setTitle("Jump to Address")
                .setView(input)
                .setPositiveButton("Go") { _, _ ->
                    try {
                        val text = input.text.toString().replace("0x", "", ignoreCase = true)
                        val address = text.toInt(16)
                        jumpToAddress(address)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid Address", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showSearchResults(results: IntArray) {
        val strResults = results.map { "0x${it.toString(16).toUpperCase().padStart(8, '0')}" }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Found ${results.size} matches")
            .setItems(strResults) { _, which ->
                val address = results[which]
                jumpToAddress(address)
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun jumpToAddress(address: Int) {
        if (address in currentBaseAddress until (currentBaseAddress + currentSize)) {
            val offset = address - currentBaseAddress
            val position = offset / 16
            hexRecyclerView.scrollToPosition(position)
        } else {
             // Try to switch region? For now just warn.
             Toast.makeText(context, "Address outside current region", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Adapter ---

    class HexListAdapter(
        private val activity: GameActivity,
        private var baseAddress: Int,
        private var size: Int
    ) : RecyclerView.Adapter<HexListAdapter.HexViewHolder>() {

        private val inflater = LayoutInflater.from(activity)

        fun updateRegion(newBase: Int, newSize: Int) {
            baseAddress = newBase
            size = newSize
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HexViewHolder {
            val view = inflater.inflate(R.layout.item_hex_row, parent, false)
            return HexViewHolder(view)
        }

        override fun onBindViewHolder(holder: HexViewHolder, position: Int) {
            val address = baseAddress + position * 16
            holder.bind(address, activity)
            
            // Remove row click listener
            holder.itemView.setOnClickListener(null)
            
            // Add touch listener to the Hex TextView to detect clicked byte
            holder.tvHex.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val textView = v as TextView
                    val layout = textView.layout
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    
                    if (layout != null) {
                        val line = layout.getLineForVertical(y)
                        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                        
                        // String format is "00 00 00 " (3 chars per byte)
                        // Byte index = offset / 3
                        val byteIndex = offset / 3
                        
                        if (byteIndex in 0..15) {
                            val targetAddress = address + byteIndex
                            // User requested 1-byte editing (8-bit)
                            // We open edit dialog for [targetAddress] as 8-bit
                            showEditByteDialog(activity, targetAddress)
                        }
                    }
                }
                true // Consume event
            }
        }

        override fun getItemCount(): Int {
             return size / 16
        }

        private fun showEditByteDialog(context: GameActivity, startAddress: Int) {
            // Read 1 byte (8-bit)
            val data = context.getMemoryRange(startAddress, 1)
            if (data == null || data.isEmpty()) return

            // Initial Value
            val byteVal = data[0].toInt() and 0xFF
            
            // Layout container
            val container = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(48, 24, 48, 24)
            }

            // Input Field
            val input = EditText(context).apply {
                setText("%02X".format(byteVal))
                hint = "XX"
                filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or android.text.InputType.TYPE_CLASS_TEXT
                setSelectAllOnFocus(true)
            }
            
            // Radio Group for Mode
            val radioGroup = android.widget.RadioGroup(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 16)
            }
            val rbHex = android.widget.RadioButton(context).apply {
                text = "Hex"
                isChecked = true
            }
            val rbDec = android.widget.RadioButton(context).apply {
                text = "Dec"
            }
            radioGroup.addView(rbHex)
            radioGroup.addView(rbDec)

            container.addView(input)
            container.addView(radioGroup)

            // Logic to switch modes
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val currentText = input.text.toString()
                var currentVal = 0
                
                try {
                    // Parse based on *previous* mode (which is opposite of checkedId)
                    // If checkedId is Dec, we just came from Hex.
                    if (checkedId == rbDec.id) {
                        // Switching Hex -> Dec
                        currentVal = if(currentText.isNotEmpty()) currentText.toInt(16) else 0
                        input.setText(currentVal.toString())
                        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        input.filters = arrayOf(android.text.InputFilter.LengthFilter(3)) // Max 255
                    } else {
                        // Switching Dec -> Hex
                        currentVal = if(currentText.isNotEmpty()) currentText.toInt(10) else 0
                        input.setText("%02X".format(currentVal))
                        input.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or android.text.InputType.TYPE_CLASS_TEXT
                        input.filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    }
                } catch (e: Exception) {
                    input.setText("0") // Reset on error
                }
                input.setSelection(input.text.length)
            }

            AlertDialog.Builder(context)
                .setTitle("Edit Byte at 0x${startAddress.toString(16).toUpperCase().padStart(8, '0')}")
                .setView(container)
                .setPositiveButton("Write") { _, _ ->
                    try {
                         val newStr = input.text.toString()
                         if (newStr.isEmpty()) return@setPositiveButton
                         
                         var newVal = 0
                         if (rbHex.isChecked) {
                             newVal = newStr.toInt(16)
                         } else {
                             newVal = newStr.toInt(10)
                         }
                         
                         val finalVal = newVal and 0xFF
                         
                         // Write single byte
                         context.writeMem8(startAddress, finalVal)
                         
                         notifyDataSetChanged()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid Value", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("Add Cheat") { _, _ ->
                     // Helper: get current value as Hex
                     var hexVal = ""
                     var fullValue: Long = 0 // Use Long to support > Int max if needed (though EditText input is limited)
                     try {
                         val newStr = input.text.toString()
                         if (rbHex.isChecked) {
                             fullValue = newStr.toLong(16)
                             hexVal = newStr // Keep original string to determine length
                         } else {
                             fullValue = newStr.toLong(10)
                             hexVal = fullValue.toString(16)
                         }
                     } catch (e: Exception) {
                         hexVal = "00"
                         fullValue = 0
                     }
                
                     // Add Cheat Code: Autodetect Length and Split
                     val byteCount = (hexVal.length + 1) / 2
                     val finalCodeBuilder = StringBuilder()
                     
                     // If value is small (<= 255), treat as single byte
                     if (fullValue <= 255) {
                         // ... Existing single byte logic ...
                         val prefix = (startAddress ushr 24) and 0xFF
                         if (prefix == 0x02 || prefix == 0x03) {
                             val cbAddr = (startAddress and 0x0FFFFFFF) or 0x30000000
                             finalCodeBuilder.append("%08X %04X".format(cbAddr, fullValue and 0xFF))
                         } else {
                             finalCodeBuilder.append("${startAddress.toString(16).toUpperCase().padStart(8, '0')}:${"%02X".format(fullValue and 0xFF)}")
                         }
                     } else {
                         // Multi-byte splitting logic
                         // We assume Little Endian? OR Big Endian?
                         // User typed "FFFFFF". Usually means [25]=FF, [26]=FF, [27]=FF.
                         // But if user typed "1234", GBA Little Endian means [25]=34, [26]=12.
                         // However, for "Cheat Code", user usually expects "Value as seen".
                         // If I write "Raw", mGBA auto-parses.
                         // But we want SECURE 8-bit writes.
                         // Let's assume input is Big Endian string ("1234" -> 0x12, 0x34) or Little Endian value?
                         // In Hex Editor, values are usually shown Big Endian (Human Readable).
                         // So "1234" -> 0x12 at Addr, 0x34 at Addr+1?
                         // NO. In standard hex editors, "1234" usually means 16-bit value 0x1234.
                         // In Little Endian memory: 34 12.
                         // So Addr=34, Addr+1=12.
                         
                         // Decision: Treat input as a Number. 
                         // Split into bytes (Little Endian for GBA).
                         var tempVal = fullValue
                         for (i in 0 until byteCount) {
                             val byte = (tempVal and 0xFF).toInt()
                             tempVal = tempVal ushr 8
                             
                             val currentAddr = startAddress + i
                             val prefix = (currentAddr ushr 24) and 0xFF
                             if (prefix == 0x02 || prefix == 0x03) {
                                 val cbAddr = (currentAddr and 0x0FFFFFFF) or 0x30000000
                                 if (finalCodeBuilder.isNotEmpty()) finalCodeBuilder.append("\n")
                                 finalCodeBuilder.append("%08X %04X".format(cbAddr, byte))
                             } else {
                                  if (finalCodeBuilder.isNotEmpty()) finalCodeBuilder.append("\n")
                                  finalCodeBuilder.append("${currentAddr.toString(16).toUpperCase().padStart(8, '0')}:${"%02X".format(byte)}")
                             }
                             
                             if (tempVal == 0L && i >= (hexVal.length / 2)) break // Stop if no more data
                         }
                     }
                     
                     showAddCheatDialogWithCode(context, finalCodeBuilder.toString())
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun showAddCheatDialogWithCode(context: GameActivity, prefilledCode: String) {
            val inputLayout = android.widget.LinearLayout(context)
            inputLayout.orientation = android.widget.LinearLayout.VERTICAL
            inputLayout.setPadding(32, 16, 32, 16)
            
            val nameInput = EditText(context)
            nameInput.hint = "Cheat Name"
            nameInput.setText("New Cheat")
            
            val codeInput = EditText(context)
            codeInput.hint = "Code"
            codeInput.setText(prefilledCode)
            
            inputLayout.addView(nameInput)
            inputLayout.addView(codeInput)
            
            AlertDialog.Builder(context)
                .setTitle("Add to Cheat List")
                .setView(inputLayout)
                .setPositiveButton("Add") { _, _ ->
                    val name = nameInput.text.toString()
                    val code = codeInput.text.toString()
                    if (name.isNotEmpty() && code.isNotEmpty()) {
                        addToCheats(context, name, code)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        private fun showAddCheatDialog(context: GameActivity, address: Int) {
            val inputLayout = android.widget.LinearLayout(context)
            inputLayout.orientation = android.widget.LinearLayout.VERTICAL
            inputLayout.setPadding(32, 16, 32, 16)
            
            val nameInput = EditText(context)
            nameInput.hint = "Cheat Name"
            nameInput.setText("New Cheat")
            
            val codeInput = EditText(context)
            codeInput.hint = "Code (Addr:Val)"
            // Default to 8-bit RAM Write for safety or generic Raw
            // mGBA Raw: XXXXXXXX:YY
            val defaultCode = "${address.toString(16).toUpperCase().padStart(8, '0')}:FF" 
            codeInput.setText(defaultCode)
            
            inputLayout.addView(nameInput)
            inputLayout.addView(codeInput)
            
            AlertDialog.Builder(context)
                .setTitle("Add to Cheat List")
                .setView(inputLayout)
                .setPositiveButton("Add") { _, _ ->
                    val name = nameInput.text.toString()
                    val code = codeInput.text.toString()
                    if (name.isNotEmpty() && code.isNotEmpty()) {
                        addToCheats(context, name, code)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun addToCheats(context: GameActivity, name: String, code: String) {
            try {
                val gamePath = context.intent.getStringExtra("gamepath")
                var gameNum = context.intent.getStringExtra("cheat")
                
                // Fallback for gameNum logic if needed (borrowed from GameActivity logic)
                // Assuming cheat extra works as it's passed from GameList
                if (gameNum.isNullOrEmpty()) {
                     // Try to parse from path? Or assume context knows.
                     // GameActivity doesn't hold gameNum publicly easily without parsing again.
                     // But CheatUtils.generateCheat checks intent.
                     // Let's assume passed correctly.
                     Toast.makeText(context, "Error: Game ID not found", Toast.LENGTH_SHORT).show()
                     return
                }

                val parentDir = if (gamePath != null) java.io.File(gamePath).parentFile else null
                
                val newCheat = hh.game.mgba_android.utils.Cheat(
                    isSelect = true,
                    cheatTitle = name,
                    cheatCode = code
                )
                
                hh.game.mgba_android.utils.CheatUtils.appendCheat(context, gameNum, newCheat, parentDir)
                Toast.makeText(context, "Cheat Added!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to add cheat: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        private fun writeBytes(context: GameActivity, startAddress: Int, hexString: String) {
            try {
                val parts = hexString.trim().split("\\s+".toRegex())
                var currentAddr = startAddress
                for (part in parts) {
                    if (part.isBlank()) continue
                    val value = part.toInt(16)
                    // JNI writeMem takes Int (32-bit value? No, writeMem takes 32-bit addr and 32-bit val, but width?)
                    // The 'writeMem' JNI function usually calls busWrite32. 
                    // Wait, if I want to write a byte, busWrite32 might overwrite neighbors?
                    // YES. busWrite32 writes 4 bytes.
                    // I need writeMem8 or generic writeMem.
                    // The current GameActivity.writeMem calls busWrite32 in C++.
                    // That is dangerous for byte editing!
                    // I MUST fix this. But I cannot change C++ in this step easily unless I rely on existing hacks.
                    // Use cheat engine logic?
                    // Actually, if I write byte-by-byte, I need busWrite8.
                    // The current writeMem (JNI) is:
                    // JNIEXPORT void JNICALL ... writeMem(..., jint address, jint value) { ... busWrite32 ... }
                    // This is BAD for editing bytes.
                    
                    // Workaround: Read 32-bit, mask, write back? No, race conditions or side effects.
                    // If I'm editing a byte, I should use 8-bit write.
                    
                    // Since I cannot change C++ in this file write, I should stick to what I have?
                    // NO. This is "Advanced Memory Tools". I must do it right.
                    // I will Assume I updated writeMem to support size or added writeMem8.
                    // BUT I haven't.
                    // Let's implement logic assuming I'll fix C++ in next step.
                    // Or, better:
                    // I'll leave a TODO here and just verify the UI works for now?
                    // No, user wants to edit.
                    // I'll call a new method `writeMem8` (which I will add to JNI/Java next).
                    context.writeMem8(currentAddr, value)
                    currentAddr++
                }
                // Refresh view
                // Since data is fetched in onBind, simply notifyItemChanged
                // We need to find the position.
                // But this is inside Adapter, so we can't easily notify efficiently without holder ref.
                // Just notifyDataSetChanged for simplicity or leave it (scrolling refreshes).
                // context.runOnUiThread { notifyDataSetChanged() } -> Can't call from here easily.
                // Actually we are on UI thread (dialog callback).
                notifyDataSetChanged()
                
            } catch (e: Exception) {
                Toast.makeText(context, "Write Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        class HexViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal val tvAddress: TextView = itemView.findViewById(R.id.tv_address)
            internal val tvHex: TextView = itemView.findViewById(R.id.tv_hex)
            private val tvAscii: TextView = itemView.findViewById(R.id.tv_ascii)

            fun bind(address: Int, activity: GameActivity) {
                tvAddress.text = address.toString(16).toUpperCase().padStart(8, '0')

                val data = activity.getMemoryRange(address, 16)
                if (data != null) {
                    val hexBuilder = StringBuilder()
                    val asciiBuilder = StringBuilder()

                    for (b in data) {
                        val byteVal = b.toInt() and 0xFF
                        hexBuilder.append("%02X ".format(byteVal))
                        if (byteVal in 32..126) asciiBuilder.append(byteVal.toChar())
                        else asciiBuilder.append('.')
                    }
                    tvHex.text = hexBuilder.toString()
                    tvAscii.text = asciiBuilder.toString()
                } else {
                    tvHex.text = "??"
                    tvAscii.text = "?"
                }
            }
        }
    }
}
