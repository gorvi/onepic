import os
import shutil
import json

ANDROID_ASSETS_DIR = "android/app/src/main/assets/gallery_levels"
IOS_RESOURCES_DIR = "ios/OnePic/OnePic/Resources/Images"

def copy_resource(filename, source_path):
    if not os.path.exists(IOS_RESOURCES_DIR):
        os.makedirs(IOS_RESOURCES_DIR)
    
    # Destination file
    dest_path = os.path.join(IOS_RESOURCES_DIR, filename)
    shutil.copy2(source_path, dest_path)
    print(f"Migrated {filename} to Resources")

def main():
    if not os.path.exists(ANDROID_ASSETS_DIR):
        print(f"Source not found: {ANDROID_ASSETS_DIR}")
        return

    count = 0
    for root, dirs, files in os.walk(ANDROID_ASSETS_DIR):
        for file in files:
            if file.lower().endswith(".webp") or file.lower().endswith(".png") or file.lower().endswith(".jpg"):
                copy_resource(file, os.path.join(root, file))
                count += 1
    
    print(f"Total migrated: {count}")

if __name__ == "__main__":
    main()
