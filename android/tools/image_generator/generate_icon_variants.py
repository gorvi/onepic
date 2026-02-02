import generate_images
import os
import requests
import base64
import time

# Use the same API key as existing config
API_KEY = generate_images.API_KEY
MODEL_NAME = generate_images.MODEL_NAME
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"

CONCEPTS = [
    {
        "filename": "../../icon_concept_a_golden_key.png",
        "prompt": """
        App icon, 1:1 square. A single, hyper-realistic golden puzzle piece with intricate internal clockwork mechanisms and glowing circuitry. 
        It floats in a deep cosmic void. The surface is polished glass and gold. 
        Cinematic lighting, 8k resolution, minimalist but premium.
        """
    },
    {
        "filename": "../../icon_concept_b_stellar_ark.png",
        "prompt": """
        App icon, 1:1 square. Epic sci-fi art. A massive, sleek silver starship (The Ark) viewed from above/side silhouette against a starry space background.
        Crucially, a section of the ship's hull is MISSING, and the void is shaped exactly like a puzzle piece, glowing with bright blue energy.
        Symbolizes building the ship. High contrast, sharp vector lines.
        """
    },
    {
        "filename": "../../icon_concept_c_cosmic_fragment.png",
        "prompt": """
        App icon, 1:1 square. A thick, dark heavy metal frame shaped like a puzzle piece.
        INSIDE the puzzle piece frame is a window into a vibrant, colorful nebula and swirling galaxy.
        The background OUTSIDE the frame is pitch black deep space.
        Contrast between the dark metal container and the infinite universe inside.
        """
    }
]

def generate_icon(prompt, filename):
    headers = { "Content-Type": "application/json" }
    payload = {
        "instances": [{ "prompt": prompt.strip() }],
        "parameters": { "sampleCount": 1, "aspectRatio": "1:1" }
    }

    print(f"Generating {filename}...")
    
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
    print("🚀 Starting Batch Icon Generation...")
    for concept in CONCEPTS:
        success = generate_icon(concept["prompt"], concept["filename"])
        if success:
            time.sleep(2) # Avoid rate limits
        else:
            time.sleep(5)
    print("🏁 Batch Generation Complete.")
