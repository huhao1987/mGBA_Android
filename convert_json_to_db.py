
import sqlite3
import json
import os

ASSETS_DIR = r"H:\androidprojects\mGBA_Android\app\src\main\assets"
CHEATS_JSON = os.path.join(ASSETS_DIR, "cheats.json")
MAPPING_JSON = os.path.join(ASSETS_DIR, "game_mapping.json")
DB_PATH = os.path.join(ASSETS_DIR, "cheats.db")

def convert():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    # Create tables
    c.execute('''
        CREATE TABLE games (
            game_id TEXT PRIMARY KEY,
            name TEXT
        )
    ''')
    
    c.execute('''
        CREATE TABLE cheats (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
            game_id TEXT,
            desc TEXT,
            code TEXT,
            enabled INTEGER,
            FOREIGN KEY(game_id) REFERENCES games(game_id)
        )
    ''')
    
    c.execute('''
        CREATE TABLE mappings (
            serial TEXT PRIMARY KEY,
            game_id TEXT,
            FOREIGN KEY(game_id) REFERENCES games(game_id)
        )
    ''')
    
    # Indices for performance
    c.execute("CREATE INDEX idx_cheats_game_id ON cheats(game_id)")
    c.execute("CREATE INDEX idx_mappings_serial ON mappings(serial)")

    print("Loading JSON files...")
    
    with open(CHEATS_JSON, "r", encoding="utf-8") as f:
        cheats_data = json.load(f)
        
    with open(MAPPING_JSON, "r", encoding="utf-8") as f:
        mapping_data = json.load(f)

    print("Inserting games and cheats...")
    
    game_entries = []
    cheat_entries = []
    
    games_dict = cheats_data.get("games", {})
    
    for game_id, game_info in games_dict.items():
        # Insert Game
        name = game_info.get("name", "")
        game_entries.append((game_id, name))
        
        # Insert Cheats
        for cheat in game_info.get("cheats", []):
            desc = cheat.get("desc", "")
            code = cheat.get("code", "")
            enabled = 1 if cheat.get("enabled", False) else 0
            cheat_entries.append((game_id, desc, code, enabled))
            
    c.executemany("INSERT INTO games (game_id, name) VALUES (?, ?)", game_entries)
    c.executemany("INSERT INTO cheats (game_id, desc, code, enabled) VALUES (?, ?, ?, ?)", cheat_entries)
    
    print(f"Inserted {len(game_entries)} games and {len(cheat_entries)} cheats.")
    
    print("Inserting mappings...")
    mapping_entries = []
    for serial, game_id in mapping_data.items():
        # Only insert mapping if game_id exists (optional constraint, but good for integrity)
        # Actually standard SQlite doesn't enforce foreign keys by default unless enabled, 
        # but let's just insert all.
        mapping_entries.append((serial, game_id))
        
    c.executemany("INSERT OR IGNORE INTO mappings (serial, game_id) VALUES (?, ?)", mapping_entries)
    print(f"Inserted {len(mapping_entries)} mappings.")

    conn.commit()
    conn.close()
    
    print(f"Database created at {DB_PATH}")

if __name__ == "__main__":
    convert()
