import os
import xml.etree.ElementTree as ET
import json
import re
import glob

# Configuration
ANDROID_RES_DIR = "android/app/src/main/res"
# Target directory for iOS JSONs
# We use the Resources/i18n directory which is added as a folder reference
IOS_I18N_DIR = "ios/OnePic/OnePic/Resources/gallery_levels/i18n/ui" 

def ensure_dir(directory):
    if not os.path.exists(directory):
        os.makedirs(directory)

def parse_android_strings(xml_path):
    strings = {}
    if not os.path.exists(xml_path):
        return strings
    
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        for string in root.findall('string'):
            name = string.get('name')
            value = string.text
            
            if name and value:
                # Handle Android formatting to iOS formatting
                # %1$s -> %@
                # %s -> %@
                # %1$d -> %d
                
                # Replace %s with %@
                val = re.sub(r'%([0-9]+\$)?s', r'%\1@', value)
                # Remove positional arguments if they are just 1$ (simple case)
                # actually iOS supports %1$@, so we might just need to change s to @
                val = val.replace('%s', '%@')
                val = re.sub(r'%([0-9]+\$)s', r'%\1@', val)
                
                # Unescape quotes
                val = val.replace("\\'", "'").replace('\\"', '"').replace('\\n', '\n')
                
                strings[name] = val
    except Exception as e:
        print(f"Error parsing {xml_path}: {e}")
        
    return strings

def main():
    print("🚀 Starting Android -> iOS Localization Sync...")
    ensure_dir(IOS_I18N_DIR)
    
    # 1. Process Default (English usually)
    default_xml = os.path.join(ANDROID_RES_DIR, "values", "strings.xml")
    if os.path.exists(default_xml):
        print(f"Processing default strings from {default_xml}...")
        en_strings = parse_android_strings(default_xml)
        
        # Merging default strings is handled in the loop below as 'values' -> 'en'
        pass
            
    # Redoing the loop with safe filenames
    
    # Map of android folder suffix to ios code
    # values-zh -> zh
    # values -> en (default)
    
    dirs = glob.glob(os.path.join(ANDROID_RES_DIR, "values*"))
    
    for d in dirs:
        dirname = os.path.basename(d)
        if dirname == "values":
            lang_code = "en"
        elif dirname.startswith("values-"):
            lang_code = dirname.replace("values-", "")
            # Handle zh-rCN -> zh-Hans if needed, usually Android uses zh-rCN or just zh
            # iOS uses zh-Hans or zh.
            # Our app uses "zh" for simplified.
            if lang_code == "zh-rCN": lang_code = "zh"
            # if lang_code == "zh": keep as zh
        else:
            continue
            
        xml_path = os.path.join(d, "strings.xml")
        if os.path.exists(xml_path):
            print(f"  Parsng {lang_code} from {dirname}...")
            strings = parse_android_strings(xml_path)
            
            # Write to ui_xx_data.json
            out_file = os.path.join(IOS_I18N_DIR, f"ui_{lang_code}_data.json")
            with open(out_file, 'w', encoding='utf-8') as f:
                json.dump(strings, f, indent=4, ensure_ascii=False, sort_keys=True)
            print(f"    -> Wrote {len(strings)} keys to {out_file}")

if __name__ == "__main__":
    main()
