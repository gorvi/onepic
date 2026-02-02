
import os
import requests
import json
import base64

# Configuration
API_KEY = "AIzaSyAbnRtUPuYJZokcrDYtuSh9Hve_-l_Dq-M" 
MODEL_NAME = "imagen-4.0-generate-001" 
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"

OUTPUT_DIR = "app/src/main/assets/gallery_levels"
FILENAME = "level_42_A_StarDragon.jpg"

# New Prompt: Cute Dinosaur
PROMPT = "A cute baby dragon with starry scales sitting on a rock, looking up at the galaxy, big eyes, adorable, pixar style, 8k, vibrant lighting, whimsical atmosphere"

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
    generate_image(PROMPT, FILENAME)
