import tkinter as tk
from tkinter import ttk, messagebox, filedialog, Menu
import sqlite3
import os

LANG = {
    "en": {
        "title": "mGBA Cheat Database Editor",
        "file": "File",
        "open_db": "Open Database",
        "language": "Language",
        "exit": "Exit",
        "games": "Games",
        "search": "Search:",
        "add_game": "Add Game",
        "edit_game": "Edit Game",
        "cheats": "Cheats",
        "cheats_for": "Cheats for:",
        "add_cheat": "Add Cheat",
        "delete_cheat": "Delete Cheat",
        "edit_cheat": "Edit Cheat",
        "desc": "Description:",
        "code": "Code (GS/PAR/Raw):",
        "enabled_default": "Enabled by Default",
        "save_changes": "Save Changes",
        "ready": "Ready",
        "loaded": "Loaded:",
        "error": "Error",
        "confirm": "Confirm",
        "delete_confirm": "Delete this cheat?",
        "select_game": "Select Game",
        "select_game_msg": "Please select a game first.",
        "game_id": "Game ID (Serial):",
        "game_name": "Game Name:",
        "missing_info": "Missing Info",
        "both_req": "Both fields required",
        "id_exists": "Game ID already exists.",
        "saved": "Saved.",
        "save_err": "Save Error",
        "db_err": "DB Error",
        "open_err": "Failed to open database",
        "file_type": "Cheat DB"
    },
    "zh": {
        "title": "mGBA 金手指数据库编辑器",
        "file": "文件",
        "open_db": "打开数据库",
        "language": "语言",
        "exit": "退出",
        "games": "游戏列表",
        "search": "搜索:",
        "add_game": "添加游戏",
        "edit_game": "编辑游戏",
        "cheats": "金手指列表",
        "cheats_for": "金手指:",
        "add_cheat": "添加金手指",
        "delete_cheat": "删除金手指",
        "edit_cheat": "编辑金手指",
        "desc": "描述:",
        "code": "代码 (GS/PAR/Raw):",
        "enabled_default": "默认开启",
        "save_changes": "保存修改",
        "ready": "就绪",
        "loaded": "已加载:",
        "error": "错误",
        "confirm": "确认",
        "delete_confirm": "删除此金手指？",
        "select_game": "选择游戏",
        "select_game_msg": "请先选择一个游戏。",
        "game_id": "游戏 ID (Serial):",
        "game_name": "游戏名称:",
        "missing_info": "信息缺失",
        "both_req": "两个字段都需要填写",
        "id_exists": "游戏 ID 已存在。",
        "saved": "已保存。",
        "save_err": "保存错误",
        "db_err": "数据库错误",
        "open_err": "无法打开数据库",
        "file_type": "金手指数据库"
    }
}

class CheatEditorApp:
    def __init__(self, root):
        self.root = root
        self.lang_code = "zh" # Default to Chinese as requested
        self.t = LANG[self.lang_code]
        
        self.root.geometry("1000x600")

        self.conn = None
        self.cursor = None
        self.current_game_id = None
        self.current_cheat_rowid = None
        
        self.ui_elements = {}

        self.setup_ui()
        self.update_texts()
        
        # Auto-load if file exists in default location
        default_db = "app/src/main/assets/gbaCheat.dat"
        if os.path.exists(default_db):
            self.load_database(default_db)
            
    def set_lang(self, code):
        self.lang_code = code
        self.t = LANG[code]
        self.create_menu()
        self.update_texts()
        
    def create_menu(self):
        # Top Menu
        self.menubar = tk.Menu(self.root)
        
        # File Menu
        self.file_menu = tk.Menu(self.menubar, tearoff=0)
        self.file_menu.add_command(label=self.t["open_db"], command=self.open_db_file)
        self.file_menu.add_separator()
        self.file_menu.add_command(label=self.t["exit"], command=self.root.quit)
        self.menubar.add_cascade(label=self.t["file"], menu=self.file_menu)
        
        # Language Menu
        lang_menu = tk.Menu(self.menubar, tearoff=0)
        lang_menu.add_command(label="English", command=lambda: self.set_lang("en"))
        lang_menu.add_command(label="中文", command=lambda: self.set_lang("zh"))
        self.menubar.add_cascade(label=self.t["language"], menu=lang_menu)
        
        self.root.config(menu=self.menubar)

    def update_texts(self):
        self.root.title(self.t["title"])
        
        # Labels & Buttons
        self.ui_elements["games_lbl"].config(text=self.t["games"])
        self.ui_elements["search_lbl"].config(text=self.t["search"])
        self.ui_elements["add_game_btn"].config(text=self.t["add_game"])
        self.ui_elements["edit_game_btn"].config(text=self.t["edit_game"])
        
        self.ui_elements["cheats_title_lbl"].config(text=self.t["cheats"])
        if self.current_game_id:
             # Refresh title with localized prefix
             name = next((g[1] for g in self.games_cache if g[0] == self.current_game_id), "?")
             self.ui_elements["cheats_title_lbl"].config(text=f"{self.t['cheats_for']} {name}")
             
        self.ui_elements["add_cheat_btn"].config(text=self.t["add_cheat"])
        self.ui_elements["del_cheat_btn"].config(text=self.t["delete_cheat"])
        
        self.ui_elements["edit_cheat_lbl"].config(text=self.t["edit_cheat"])
        self.ui_elements["desc_lbl"].config(text=self.t["desc"])
        self.ui_elements["code_lbl"].config(text=self.t["code"])
        self.ui_elements["enabled_chk"].config(text=self.t["enabled_default"])
        self.ui_elements["save_btn"].config(text=self.t["save_changes"])

    def setup_ui(self):
        self.create_menu()

        # Main Layout (PanedWindow)
        paned_window = ttk.PanedWindow(self.root, orient=tk.HORIZONTAL)
        paned_window.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # --- Left Panel: Games List ---
        left_frame = ttk.Frame(paned_window, width=300)
        paned_window.add(left_frame, weight=1)

        self.ui_elements["games_lbl"] = ttk.Label(left_frame, text="Games")
        self.ui_elements["games_lbl"].pack(pady=5)
        
        # Search
        search_frame = ttk.Frame(left_frame)
        search_frame.pack(fill=tk.X, padx=5)
        self.search_var = tk.StringVar()
        self.search_var.trace("w", self.filter_games)
        
        self.ui_elements["search_lbl"] = ttk.Label(search_frame, text="Search:")
        self.ui_elements["search_lbl"].pack(anchor=tk.W)
        ttk.Entry(search_frame, textvariable=self.search_var).pack(fill=tk.X)

        # Game List Container with Scrollbar
        game_list_frame = ttk.Frame(left_frame)
        game_list_frame.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        self.game_listbox = tk.Listbox(game_list_frame, exportselection=False)
        self.game_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        game_scroll = ttk.Scrollbar(game_list_frame, orient=tk.VERTICAL, command=self.game_listbox.yview)
        game_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.game_listbox.config(yscrollcommand=game_scroll.set)
        
        self.game_listbox.bind("<<ListboxSelect>>", self.on_game_select)

        # Game Buttons
        game_btn_frame = ttk.Frame(left_frame)
        game_btn_frame.pack(fill=tk.X, padx=5, pady=5)
        
        self.ui_elements["add_game_btn"] = ttk.Button(game_btn_frame, text="Add Game", command=self.add_game)
        self.ui_elements["add_game_btn"].pack(side=tk.LEFT, fill=tk.X, expand=True)
        
        self.ui_elements["edit_game_btn"] = ttk.Button(game_btn_frame, text="Edit Game", command=self.edit_game)
        self.ui_elements["edit_game_btn"].pack(side=tk.LEFT, fill=tk.X, expand=True)

        # --- Middle Panel: Cheats List ---
        middle_frame = ttk.Frame(paned_window, width=300)
        paned_window.add(middle_frame, weight=1)

        self.ui_elements["cheats_title_lbl"] = ttk.Label(middle_frame, text="Cheats", font=("Arial", 10, "bold"))
        self.ui_elements["cheats_title_lbl"].pack(pady=5)

        # Cheat List Container with Scrollbar
        cheat_list_frame = ttk.Frame(middle_frame)
        cheat_list_frame.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        self.cheat_listbox = tk.Listbox(cheat_list_frame, exportselection=False)
        self.cheat_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        cheat_scroll = ttk.Scrollbar(cheat_list_frame, orient=tk.VERTICAL, command=self.cheat_listbox.yview)
        cheat_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.cheat_listbox.config(yscrollcommand=cheat_scroll.set)
        
        self.cheat_listbox.bind("<<ListboxSelect>>", self.on_cheat_select)

        cheat_btn_frame = ttk.Frame(middle_frame)
        cheat_btn_frame.pack(fill=tk.X, padx=5, pady=5)
        
        self.ui_elements["add_cheat_btn"] = ttk.Button(cheat_btn_frame, text="Add Cheat", command=self.add_cheat)
        self.ui_elements["add_cheat_btn"].pack(side=tk.LEFT, fill=tk.X, expand=True)
        
        self.ui_elements["del_cheat_btn"] = ttk.Button(cheat_btn_frame, text="Delete Cheat", command=self.delete_cheat)
        self.ui_elements["del_cheat_btn"].pack(side=tk.LEFT, fill=tk.X, expand=True)

        # --- Right Panel: Editor ---
        right_frame = ttk.Frame(paned_window, width=400)
        paned_window.add(right_frame, weight=2)

        self.ui_elements["edit_cheat_lbl"] = ttk.Label(right_frame, text="Edit Cheat")
        self.ui_elements["edit_cheat_lbl"].pack(pady=5)

        # Fields
        self.ui_elements["desc_lbl"] = ttk.Label(right_frame, text="Description:")
        self.ui_elements["desc_lbl"].pack(anchor=tk.W, padx=5)
        self.desc_entry = ttk.Entry(right_frame)
        self.desc_entry.pack(fill=tk.X, padx=5, pady=2)

        self.ui_elements["code_lbl"] = ttk.Label(right_frame, text="Code (GS/PAR/Raw):")
        self.ui_elements["code_lbl"].pack(anchor=tk.W, padx=5)
        self.code_text = tk.Text(right_frame, height=15)
        self.code_text.pack(fill=tk.BOTH, expand=True, padx=5, pady=2)

        self.enabled_var = tk.BooleanVar()
        self.ui_elements["enabled_chk"] = ttk.Checkbutton(right_frame, text="Enabled by Default", variable=self.enabled_var)
        self.ui_elements["enabled_chk"].pack(anchor=tk.W, padx=5, pady=5)

        # Save Button
        self.ui_elements["save_btn"] = ttk.Button(right_frame, text="Save Changes", command=self.save_current_cheat, state=tk.DISABLED)
        self.ui_elements["save_btn"].pack(fill=tk.X, padx=5, pady=10)

        self.status_bar = ttk.Label(self.root, text="Ready", relief=tk.SUNKEN, anchor=tk.W)
        self.status_bar.pack(side=tk.BOTTOM, fill=tk.X)

    def load_database(self, path):
        try:
            if self.conn:
                self.conn.close()
            self.conn = sqlite3.connect(path)
            self.cursor = self.conn.cursor()
            self.load_games()
            self.status_bar.config(text=f"{self.t['loaded']} {path}")
        except Exception as e:
            messagebox.showerror(self.t["error"], f"{self.t['open_err']}: {e}")

    def open_db_file(self):
        filename = filedialog.askopenfilename(filetypes=[(self.t.get("file_type", "Cheat DB"), "*.dat *.db"), ("All Files", "*.*")])
        if filename:
            self.load_database(filename)

    def load_games(self):
        if not self.cursor: return
        self.games_cache = [] 
        try:
            self.cursor.execute("SELECT game_id, name FROM games ORDER BY name")
            self.games_cache = self.cursor.fetchall()
            self.filter_games()
        except sqlite3.Error as e:
             messagebox.showerror(self.t["db_err"], str(e))

    def filter_games(self, *args):
        search_term = self.search_var.get().lower()
        self.game_listbox.delete(0, tk.END)
        self.start_index_map = [] 
        
        for idx, (game_id, name) in enumerate(self.games_cache):
            display_str = f"[{game_id}] {name}"
            if search_term in display_str.lower():
                self.game_listbox.insert(tk.END, display_str)
                self.start_index_map.append(idx)

    def on_game_select(self, event):
        selection = self.game_listbox.curselection()
        if not selection: return
        
        list_idx = selection[0]
        cache_idx = self.start_index_map[list_idx]
        self.current_game_id = self.games_cache[cache_idx][0]
        game_name = self.games_cache[cache_idx][1]
        
        self.ui_elements["cheats_title_lbl"].config(text=f"{self.t['cheats_for']} {game_name}")
        self.load_cheats(self.current_game_id)
        
        self.clear_editor()
        self.ui_elements["save_btn"].config(state=tk.DISABLED)

    def load_cheats(self, game_id):
        self.cheat_listbox.delete(0, tk.END)
        self.cheats_cache = [] 
        
        self.cursor.execute("SELECT rowid, desc, code, enabled FROM cheats WHERE game_id = ?", (game_id,))
        self.cheats_cache = self.cursor.fetchall()
        
        for _, desc, _, enabled in self.cheats_cache:
            prefix = "[x] " if enabled else "[ ] "
            self.cheat_listbox.insert(tk.END, prefix + desc)

    def on_cheat_select(self, event):
        selection = self.cheat_listbox.curselection()
        if not selection: return
        
        index = selection[0]
        rowid, desc, code, enabled = self.cheats_cache[index]
        self.current_cheat_rowid = rowid
        
        self.desc_entry.delete(0, tk.END)
        self.desc_entry.insert(0, desc)
        
        self.code_text.delete("1.0", tk.END)
        self.code_text.insert("1.0", code)
        
        self.enabled_var.set(bool(enabled))
        self.ui_elements["save_btn"].config(state=tk.NORMAL)

    def clear_editor(self):
        self.current_cheat_rowid = None
        self.desc_entry.delete(0, tk.END)
        self.code_text.delete("1.0", tk.END)
        self.enabled_var.set(False)

    def save_current_cheat(self):
        if not self.current_cheat_rowid or not self.current_game_id: return
        
        desc = self.desc_entry.get()
        code = self.code_text.get("1.0", tk.END).strip()
        enabled = 1 if self.enabled_var.get() else 0
        
        try:
            self.cursor.execute("UPDATE cheats SET desc=?, code=?, enabled=? WHERE rowid=?", 
                                (desc, code, enabled, self.current_cheat_rowid))
            self.conn.commit()
            self.status_bar.config(text=self.t["saved"])
            self.load_cheats(self.current_game_id) 
            self.clear_editor()
        except sqlite3.Error as e:
            messagebox.showerror(self.t["save_err"], str(e))

    def add_cheat(self):
        if not self.current_game_id:
            messagebox.showinfo(self.t["select_game"], self.t["select_game_msg"])
            return

        try:
            self.cursor.execute("INSERT INTO cheats (game_id, desc, code, enabled) VALUES (?, ?, ?, ?)",
                                (self.current_game_id, "New Cheat", "", 0))
            self.conn.commit()
            self.load_cheats(self.current_game_id)
            self.cheat_listbox.selection_set(tk.END)
            self.cheat_listbox.event_generate("<<ListboxSelect>>")
        except sqlite3.Error as e:
             messagebox.showerror(self.t["error"], str(e))

    def delete_cheat(self):
        if not self.current_cheat_rowid: return
        if messagebox.askyesno(self.t["confirm"], self.t["delete_confirm"]):
            try:
                self.cursor.execute("DELETE FROM cheats WHERE rowid=?", (self.current_cheat_rowid,))
                self.conn.commit()
                self.load_cheats(self.current_game_id)
                self.clear_editor()
            except sqlite3.Error as e:
                messagebox.showerror(self.t["error"], str(e))
                
    def add_game(self):
         self.show_game_dialog(None, None)

    def edit_game(self):
        if not self.current_game_id:
            messagebox.showinfo(self.t["select_game"], self.t["select_game_msg"])
            return
        # Find current name
        current_name = next((g[1] for g in self.games_cache if g[0] == self.current_game_id), "")
        self.show_game_dialog(self.current_game_id, current_name)

    def show_game_dialog(self, game_id_val, game_name_val):
         is_edit = game_id_val is not None
         title = self.t["edit_game"] if is_edit else self.t["add_game"]
         
         top = tk.Toplevel(self.root)
         top.title(title)
         
         ttk.Label(top, text=self.t["game_id"]).grid(row=0, column=0, padx=5, pady=5)
         id_entry = ttk.Entry(top)
         id_entry.grid(row=0, column=1, padx=5, pady=5)
         if is_edit: id_entry.insert(0, game_id_val)
         
         ttk.Label(top, text=self.t["game_name"]).grid(row=1, column=0, padx=5, pady=5)
         name_entry = ttk.Entry(top)
         name_entry.grid(row=1, column=1, padx=5, pady=5)
         if is_edit: name_entry.insert(0, game_name_val)
         
         def save():
             gid = id_entry.get().strip()
             name = name_entry.get().strip()
             if not gid or not name:
                 messagebox.showwarning(self.t["missing_info"], self.t["both_req"])
                 return
             
             try:
                 if is_edit:
                     # Update games
                     self.cursor.execute("UPDATE games SET game_id=?, name=? WHERE game_id=?", (gid, name, game_id_val))
                     
                     # Update cheats foreign key if ID changed
                     if gid != game_id_val:
                         self.cursor.execute("UPDATE cheats SET game_id=? WHERE game_id=?", (gid, game_id_val))
                         
                     self.conn.commit()
                     self.current_game_id = gid # Update selection pointer
                 else:
                     self.cursor.execute("INSERT INTO games (game_id, name) VALUES (?, ?)", (gid, name))
                     self.conn.commit()
                 
                 self.load_games()
                 top.destroy()
                 
                 # Reselect if edit
                 if is_edit and self.current_game_id:
                      # Find index
                      new_idx_map = [i for i, g in enumerate(self.games_cache) if g[0] == gid]
                      if new_idx_map:
                          real_idx = new_idx_map[0]
                          # Find listbox index.. naive approach
                          # Just clear selection or complex re-select logic
                          # For now, let user see updated list
                          pass

             except sqlite3.IntegrityError:
                 messagebox.showerror(self.t["error"], self.t["id_exists"])
             except sqlite3.Error as e:
                 messagebox.showerror(self.t["error"], str(e))

         ttk.Button(top, text=self.t["save_changes"], command=save).grid(row=2, column=0, columnspan=2, pady=10)

if __name__ == "__main__":
    root = tk.Tk()
    app = CheatEditorApp(root)
    root.mainloop()
