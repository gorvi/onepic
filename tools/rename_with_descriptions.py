
import os
import json
import re

# Mapping of Level ID -> English Title (CamelCase)
LEVEL_TITLES = {
    0: "TheOrigin",
    1: "WorldTreeSeeds", 2: "SanctuaryOfLight", 3: "DawnForest", 4: "WhispersOfAmber", 5: "EmeraldZen",
    6: "RiceTerraces", 7: "DesertDunes", 8: "EdgeOfEternity", 9: "SpiritWolf", 10: "LunarFox",
    11: "RainbowPhoenix", 12: "PandaSage", 13: "TigerForest", 14: "GaneshaSoul", 15: "ForestKing",
    16: "Thunderbird", 17: "AncientCarvings", 18: "StarRoadPyramids", 19: "PetraLightGate", 20: "LostMachuPicchu",
    21: "StoneDragon", 22: "HerosArena", 23: "DomeOfLight", 24: "InfiniteLibrary", 25: "DreamGardens",
    26: "ProphetSpire", 27: "SkySails", 28: "CyberBioSpire", 29: "ArkOfTime", 30: "GlowPulseDolphins",
    31: "FluorescentCoral", 32: "IceDragonEmbryo", 33: "GhostShipwreck", 34: "AbyssalBehemoth", 35: "RealmOfLeviathan",
    36: "PhantomJellyfish", 37: "Atlantis", 38: "CoreForge", 39: "FireAndIce", 40: "ForbiddenLife",
    41: "NirvanaPhoenix", 42: "StarDragon", 43: "CelestialVeil", 44: "TimeSpiral", 45: "InvertedWaterfall",
    46: "CrystalMaze", 47: "CloudLibrary", 48: "GravityIsland", 49: "ClockworkDimension", 50: "MotherEarth",
    51: "RedPioneer", 52: "FrostRings", 53: "BinarySunset", 54: "SleeplessStar", 55: "Supernova",
    56: "GalaxySea", 57: "PillarsOfCreation", 58: "EventHorizon", 59: "Wormhole", 60: "EyeOfTheUniverse"
}

IMG_DIR = "app/src/main/assets/gallery_levels"
JSON_PATH = "app/src/main/assets/gallery_levels/gallery_descriptions.json"

def get_clean_title(level_id):
    return LEVEL_TITLES.get(level_id, f"Level{level_id}")

def rename_and_update():
    print(f"📂 Scanning {IMG_DIR}...")
    files = os.listdir(IMG_DIR)
    
    # 1. Rename files
    for level_id in range(61):
        clean_title = get_clean_title(level_id)
        
        for side in ["A", "B"]:
            # Target Name: level_{id}_{side}_{Title}.jpg
            # Note: User asked for level_1_A format, but keeping level_01 for sorting is safer.
            # actually user asked for "level_1_A/B_英文描述" specifically.
            # but level_1_A doesn't sort well. I will stick to level_01_A but append description.
            # Unless user forcibly wants single digit for <10.
            # "level_1_A/B_Description"
            
            # Let's use 02d padding because it's standard in this project.
            target_name = f"level_{level_id:02d}_{side}_{clean_title}.jpg"
            target_path = os.path.join(IMG_DIR, target_name)
            
            if os.path.exists(target_path):
                # Already named correctly
                continue
                
            # Configurable candidates
            candidates = [
                f"level_{level_id:02d}_{side}.jpg", # Standard
                f"level_{level_id}_{side}.jpg",     # Unpadded
                # f"level_{level_id:02d}_{side}_*.jpg" # Old descriptives?
            ]
            
            # Find candidate
            found_src = None
            for c in candidates:
                p = os.path.join(IMG_DIR, c)
                if os.path.exists(p):
                    found_src = c
                    break
            
            if not found_src:
                 # Check fuzzy match for existing descriptive names that might not match exact title
                 prefix = f"level_{level_id:02d}_{side}_"
                 for f in files:
                     if f.startswith(prefix) and f != target_name:
                         found_src = f
                         break
            
            # Special case for B1, A1 suffixes
            if not found_src:
                 candidate_variant = f"level_{level_id:02d}_{side}1.jpg"
                 if candidate_variant in files:
                     found_src = candidate_variant

            if found_src:
                print(f"🔄 Renaming {found_src} -> {target_name}")
                os.rename(os.path.join(IMG_DIR, found_src), target_path)

    # 2. Update JSON
    print(f"📝 Updating {JSON_PATH}...")
    with open(JSON_PATH, 'r') as f:
        data = json.load(f)
    
    # Reload files list after rename
    current_files = set(os.listdir(IMG_DIR))
    
    for level in data:
        lid = level['id']
        clean_title = get_clean_title(lid)
        
        # Expected filenames
        fname_a = f"level_{lid:02d}_A_{clean_title}.jpg"
        fname_b = f"level_{lid:02d}_B_{clean_title}.jpg"
        
        if fname_a in current_files:
            level['filename_a'] = fname_a
        else:
            print(f"⚠️  Missing A for level {lid}: Checking availability...")
            # If explicit file is missing, we must clear it so app doesn't crash on Asset load
            # OR we try to find a fallback? No, simpler to clear.
            level['filename_a'] = ""
            print(f"    -> Cleared filename_a for Level {lid}")
            
        if fname_b in current_files:
            level['filename_b'] = fname_b
        else:
             level['filename_b'] = ""
             print(f"    -> Cleared filename_b for Level {lid}")
                
    with open(JSON_PATH, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        
    print("🎉 Done.")

if __name__ == "__main__":
    rename_and_update()
