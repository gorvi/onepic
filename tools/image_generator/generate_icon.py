import generate_images
import os
import requests
import base64

# Use the same API key as existing config
API_KEY = generate_images.API_KEY
MODEL_NAME = generate_images.MODEL_NAME
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"

# Advanced, rich prompt for "Premium" look
PROMPT = """
A masterpiece application icon for a sci-fi puzzle game (OnePic). 
Centerpiece: A single, hyper-realistic golden puzzle piece with intricate internal clockwork mechanisms and glowing circuitry. The surface has a polished glassmorphism finish reflecting a cosmic nebula.
Background: A deep, immersive void of space with rich royal purple and midnight blue gradients. Swirling stardust and faint constellation lines connect in the background.
Lighting: Dramatic volumetric lighting hitting the puzzle piece from the top-left, creating a "Hero" effect with lens flares and cinematic bloom.
Style: 8k resolution, Unreal Engine 5 render style, Octane Render, sharp vector-like edges for the icon shape, vibrant and high-contrast. 
The overall feel is mysterious, intelligent, and premium.
"""

def generate_icon(prompt, filename):
    headers = {
        "Content-Type": "application/json"
    }
    
    payload = {
        "instances": [
            {
                "prompt": prompt
            }
        ],
        "parameters": {
            "sampleCount": 1,
            "aspectRatio": "1:1" 
        }
    }

    print(f"Generating Premium Icon: {filename}...")
    print(f"Prompt: {prompt.strip()}")
    
    try:
        response = requests.post(API_URL, headers=headers, json=payload)
        
        if response.status_code == 200:
            result = response.json()
            if "predictions" in result:
                prediction = result["predictions"][0]
                b64_data = prediction.get("bytesBase64Encoded") or prediction.get("bytes_base64_encoded")

                if b64_data:
                    image_data = base64.b64decode(b64_data)
                    with open(filename, "wb") as f:
                        f.write(image_data)
                    print(f"✅ Saved to: {filename}")
                    return True
                else:
                    print(f"❌ No base64 data found.")
            else:
                print(f"❌ Error parsing response: {result}")
        else:
            print(f"❌ API Error {response.status_code}: {response.text}")
            
    except Exception as e:
        print(f"❌ Exception: {e}")
    
    return False

if __name__ == "__main__":
    # Save to project root
    OUTPUT_FILE = "../../app_icon_512.png" 
    generate_icon(PROMPT, OUTPUT_FILE)
