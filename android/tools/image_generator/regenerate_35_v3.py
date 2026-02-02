
import os
import requests
import json
import base64
import time

# Configuration
API_KEY = "AIzaSyAbnRtUPuYJZokcrDYtuSh9Hve_-l_Dq-M" 
MODEL_NAME = "imagen-4.0-generate-001" 
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"

OUTPUT_DIR = "app/src/main/assets/gallery_levels"

# New Prompts for Level 35 (Real vs Cosmic)
ITEMS = [
    {
        "filename": "level_35_A_CyberKraken.jpg", # Keep filename consistent for app loading
        "prompt": "A giant squid (Architeuthis) swimming in the deep ocean darkness, realistic skin texture, large intelligent eye, bioluminescent flashes, 8k, underwater photography style, mysterious, national geographic aesthetic."
    },
    {
        "filename": "level_35_B_VoidKraken.jpg", # Keep filename consistent for app loading
        "prompt": "A cosmic kraken entity made of glowing data streams and starlight, tentacles spanning across a galaxy, neon purple and cyan, sci-fi concept art, ethereal, ascended form, hyper-realistic cgi."
    }
]

def generate_image(prompt, filename):
    headers = { "Content-Type": "application/json" }
    payload = {
        "instances": [{ "prompt": prompt }],
        "parameters": { "sampleCount": 1, "aspectRatio": "3:4" }
    }
    
    print(f"Generating: {filename} with prompt: '{prompt}'...")
    
    try:
        response = requests.post(API_URL, headers=headers, json=payload)
        if response.status_code == 200:
            result = response.json()
            if "predictions" in result:
                prediction = result["predictions"][0]
                b64_data = prediction.get("bytesBase64Encoded") or prediction.get("bytes_base64_encoded")
                
                if b64_data:
                    image_data = base64.b64decode(b64_data)
                    output_path = os.path.join(OUTPUT_DIR, filename)
                    with open(output_path, "wb") as f:
                        f.write(image_data)
                    print(f"✅ Saved to: {output_path}")
                    return True
        else:
            print(f"❌ API Error {response.status_code}: {response.text}")
    except Exception as e:
        print(f"❌ Exception: {e}")
    return False

if __name__ == "__main__":
    for item in ITEMS:
        if generate_image(item["prompt"], item["filename"]):
            time.sleep(2) # Cooldown
