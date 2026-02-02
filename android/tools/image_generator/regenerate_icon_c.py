import generate_images
import os
import requests
import base64

# API Config
API_KEY = generate_images.API_KEY
MODEL_NAME = generate_images.MODEL_NAME
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"

FILENAME = "../../icon_concept_c_max_fill.png"

# Revised Prompt: MAX FILL, SHARP IMAGE
PROMPT = """
App icon, 1:1 square. 
The image is a FULLY FILLED square of spectacular Sci-Fi Nebula (Deep Purple, Blue, Gold).
There are NO BORDERS, NO PADDING, NO BLACK EDGES.
Superimposed in the center is a LARGE, TRANSLUCENT Crystal Puzzle Piece.
The puzzle piece is ZOOMED IN so it fills 90% of the canvas.
The corners of the IMAGE CANVAS are SHARP (Not rounded).
High definition, 8k, vibrant colors, cinematic space art.
"""

def generate_icon(prompt, filename):
    headers = { "Content-Type": "application/json" }
    payload = {
        "instances": [{ "prompt": prompt.strip() }],
        "parameters": { "sampleCount": 1, "aspectRatio": "1:1" }
    }

    print(f"Generating Max Fill Icon: {filename}...")
    
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
    generate_icon(PROMPT, FILENAME)
