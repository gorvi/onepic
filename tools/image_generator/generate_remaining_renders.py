import os
import requests
import base64
import time

# Configuration from generate_images.py
API_KEY = "AIzaSyAbnRtUPuYJZokcrDYtuSh9Hve_-l_Dq-M"
MODEL_NAME = "imagen-4.0-generate-001" 
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:predict?key={API_KEY}"
OUTPUT_DIR = "app/src/main/assets/blueprint_renders"

def generate_image(prompt, filename):
    headers = {"Content-Type": "application/json"}
    payload = {
        "instances": [{"prompt": prompt}],
        "parameters": {"sampleCount": 1, "aspectRatio": "1:1"} 
    }

    print(f"🚀 Generating: {filename}...")
    try:
        response = requests.post(API_URL, headers=headers, json=payload)
        if response.status_code == 200:
            result = response.json()
            if "predictions" in result:
                prediction = result["predictions"][0]
                b64_data = prediction.get("bytesBase64Encoded") or prediction.get("bytes_base64_encoded")
                if b64_data:
                    image_data = base64.b64decode(b64_data)
                    os.makedirs(OUTPUT_DIR, exist_ok=True)
                    output_path = os.path.join(OUTPUT_DIR, filename)
                    with open(output_path, "wb") as f:
                        f.write(image_data)
                    print(f"✅ Saved to: {output_path}")
                    return True
        print(f"❌ API Error {response.status_code}: {response.text}")
    except Exception as e:
        print(f"❌ Exception: {e}")
    return False

if __name__ == "__main__":
    tasks = [
        ("A high-detail 3D rendering of the Starship Bio-Dome. Clear geodesic crystal dome containing tiny stylized emerald-green trees and white clouds. Mounted on a pearl-white rounded base. Pixar-style toy-world ecosystem. Space background.", "render_05.png"),
        ("A high-detail 3D rendering of the Communication Spire. A sleek pearl-white needle tower with star-shaped antenna arrays unfolding. Glowing pastel cyan signal nodes. Pixar-style aerospace design in a soft purple nebula.", "render_09.png")
    ]
    
    for prompt, name in tasks:
        if generate_image(prompt, name):
            time.sleep(2)
        else:
            print(f"Failed to generate {name}")
