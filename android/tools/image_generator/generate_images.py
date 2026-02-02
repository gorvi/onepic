import os
import requests
import json
import time

# Configuration
API_KEY = "AIzaSyAbnRtUPuYJZokcrDYtuSh9Hve_-l_Dq-M"  # Provided by user
# Model: Trying Imagen 3 endpoint first as 'Gemini 3.0 Pro' likely refers to the latest generation capability
# Common endpoints: 
# - images: https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-001:predict
# - text/multimodal: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
# Model: Imagen 4.0 Standard (Different quota bucket?)
MODEL_NAME = "imagen-4.0-generate-001" 
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"

# Path relative to project root (where script is run from)
OUTPUT_DIR = "app/src/main/assets/gallery_levels"

def generate_image(prompt, filename):
    headers = {
        "Content-Type": "application/json"
    }
    
    # Imagen API Payload
    payload = {
        "instances": [
            {
                "prompt": prompt
            }
        ],
        "parameters": {
            "sampleCount": 1,
            "aspectRatio": "3:4" 
        }
    }

    print(f"Generating: {filename}...")
    
    try:
        response = requests.post(API_URL, headers=headers, json=payload)
        
        if response.status_code == 200:
            result = response.json()
            # Imagen Response: predictions[0].bytesBase64Encoded
            if "predictions" in result:
                import base64
                # Check formatting. sometimes prediction is dict
                prediction = result["predictions"][0]
                b64_data = prediction.get("bytesBase64Encoded")
                
                if not b64_data and "bytes_base64_encoded" in prediction:
                     b64_data = prediction["bytes_base64_encoded"]

                if b64_data:
                    image_data = base64.b64decode(b64_data)
                    
                    # Logic to separate folder based on Level ID
                    target_dir = OUTPUT_DIR
                    
                    # Extract ID from filename: level_61_A_...
                    match = re.search(r'level_(\d+)_', filename)
                    if match:
                        level_id = int(match.group(1))
                        if level_id > 60:
                             target_dir = "app/src/main/assets/project_exodus"
                    
                    os.makedirs(target_dir, exist_ok=True)
                    output_path = os.path.join(target_dir, filename)
                    with open(output_path, "wb") as f:
                        f.write(image_data)
                    print(f"✅ Saved to: {output_path}")
                    return True
                else:
                    print(f"❌ No base64 data in prediction: {prediction}")
            else:
                print(f"❌ Error parsing response (No predictions): {result}")
        else:
            print(f"❌ API Error {response.status_code}: {response.text}")
            
    except Exception as e:
        print(f"❌ Exception: {e}")
    
    return False


import re

# ... (Previous Image Gen Logic) ...

ARTIFACTS_DIR = "/Users/ghw/.gemini/antigravity/brain/4a212706-d1ce-4809-9756-0a8551c8b350"
PROMPT_FILES = [
    "image_generation_prompts_part1.md",
    "image_generation_prompts_part2.md",
    "image_generation_prompts_part3.md"
]

def parse_prompts_from_md(file_path):
    prompts = []
    current_level_id = None
    
    print(f"📂 Parsing {file_path}...")
    
    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
        
    # Regex to capture prompts
    # Format: * **01_A (Realistic)**:
    #         > Prompt text
    
    # Valid formats:
    # ### Level 54: Sleepless Star (不夜之星)
    title_pattern = re.compile(r'### Level \d+: (.+?) \(')
    
    id_pattern = re.compile(r'\*\s+\*\*(\d+_[AB])\s+\((.+?)\)\*\*')
    prompt_pattern = re.compile(r'\s+>\s+(.+)')
    
    current_filename = None
    current_title_slug = "Unknown"
    
    for line in lines:
        # 1. Check for Level Title
        title_match = title_pattern.search(line)
        if title_match:
            raw_title = title_match.group(1).strip()
            # Convert "Sleepless Star" -> "SleeplessStar"
            current_title_slug = raw_title.replace(" ", "")
            continue

        # 2. Check for ID
        id_match = id_pattern.search(line)
        if id_match:
            # Found a new ID, e.g., 01_A
            raw_id = id_match.group(1) # 01_A
            
            # Construct filename: level_01_A_SleeplessStar.jpg
            current_filename = f"level_{raw_id}_{current_title_slug}.jpg"
            continue
            
        # 3. Check for Prompt
        prompt_match = prompt_pattern.search(line)
        if prompt_match and current_filename:
            prompt_text = prompt_match.group(1).strip()
            prompts.append((current_filename, prompt_text))
            current_filename = None # Reset
            
    return prompts

def main():
    all_prompts = []
    
    # 1. Parse Files
    for md_file in PROMPT_FILES:
        path = os.path.join(ARTIFACTS_DIR, md_file)
        if os.path.exists(path):
            all_prompts.extend(parse_prompts_from_md(path))
        else:
            print(f"⚠️ File not found: {path}")

    # 2. Add manual overrides for Level 0 if not parsing correctly (Part 1 has them)
    # The parser above handles the simplified format in the lists.
    # Level 0 in the "Missing Assets" section might have different formatting.
    # Let's add them manually to be safe if parsing fails, or trust the parser if I standardized the MD.
    # The MD format for 00_A was: * **00_A (Realistic)**:
    # So the regex should catch it.
    
    print(f"found {len(all_prompts)} prompts.")
    
    # 3. Generate
    print("🚀 Starting Batch Generation...")
    for filename, prompt in all_prompts:
        # Filter for specific levels if needed
        # if "level_54_A" not in filename:
        #     continue
            
        # Determine output path for existence check
        target_dir = OUTPUT_DIR
        match = re.search(r'level_(\d+)_', filename)
        if match:
            level_id = int(match.group(1))
            if level_id > 60:
                 target_dir = "app/src/main/assets/project_exodus"

        out_path = os.path.join(target_dir, filename)
        if os.path.exists(out_path):
             print(f"⏭️  Skipping {filename} (Already exists)")
             continue
        
        print(f"✨ Generating: {filename}")
            
        success = generate_image(prompt, filename)
        if success:
            time.sleep(1) # 1s delay
        else:
            time.sleep(5) # Longer delay on error
            
if __name__ == "__main__":
    main()

