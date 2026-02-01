
import os
import re
import shutil

# Directory containing the images
IMG_DIR = "app/src/main/assets/gallery_levels"

def normalize_filenames():
    print(f"📂 Scanning {IMG_DIR}...")
    
    files = os.listdir(IMG_DIR)
    
    # Track actions
    renamed_count = 0
    kept_count = 0
    
    # Iterate levels 0 to 60
    for level_id in range(61):
        for side in ["A", "B"]:
            target_name = f"level_{level_id:02d}_{side}.jpg"
            target_path = os.path.join(IMG_DIR, target_name)
            
            # 1. Check if target exists
            if os.path.exists(target_path):
                # print(f"✅ {target_name} exists.")
                kept_count += 1
                
                # OPTIONAL: ID duplicate old files and remove them?
                # For now, let's just focus on ensuring the target exists.
                continue
            
            # 2. Target missing, look for legacy/alternative naming
            # Pattern: level_{id}_{side}_*.jpg (but NOT level_{id}_{side}.jpg because we checked)
            # Or formatted differently?
            # User files seen: level_XX_A_Name.jpg
            
            prefix = f"level_{level_id}_{side}_" # Single digit?
            prefix_padded = f"level_{level_id:02d}_{side}_"
            
            candidate = None
            
            for f in files:
                # Check strict prefix match to avoid matching level_1_A vs level_10_A
                if f.startswith(f"level_{level_id}_{side}_") or f.startswith(f"level_{level_id:02d}_{side}_") or f == f"level_{level_id}_{side}.jpg":
                     # Ensure it's not a different level (e.g. 1 matching 10 is unlikely with underscore)
                     # But level_1_A_... vs level_10...
                     # level_1_A_ matches level_1_A_...
                     # level_10... does not start with level_1_...
                     
                     # Exclude the target name itself (though we know it doesn't exist)
                     if f == target_name: continue
                     
                     candidate = f
                     break
            
            if candidate:
                src_path = os.path.join(IMG_DIR, candidate)
                print(f"🔄 Renaming {candidate} -> {target_name}")
                os.rename(src_path, target_path)
                renamed_count += 1
            else:
                 # print(f"⚠️  Missing {target_name} and no candidate found.")
                 pass

    print(f"🎉 Done. Renamed {renamed_count} files. Kept {kept_count} existing standard files.")

if __name__ == "__main__":
    normalize_filenames()
