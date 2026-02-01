import requests
import json

API_KEY = "AIzaSyAbnRtUPuYJZokcrDYtuSh9Hve_-l_Dq-M"
API_URL = f"https://generativelanguage.googleapis.com/v1beta/models?key={API_KEY}"

def list_models():
    try:
        response = requests.get(API_URL)
        if response.status_code == 200:
            models = response.json().get('models', [])
            print(f"Found {len(models)} models:")
            for m in models:
                print(f"- {m['name']}: {m['supportedGenerationMethods']}")
        else:
            print(f"Error {response.status_code}: {response.text}")
    except Exception as e:
        print(f"Exception: {e}")

if __name__ == "__main__":
    list_models()
