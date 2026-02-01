#!/usr/bin/env python3
"""Merge locale strings.xml with full template from values/strings.xml.
   For each locale: keep existing translations, fill missing keys with English from values/.
"""
import re
import os

RES_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
VALUES_DEFAULT = os.path.join(RES_DIR, "values", "strings.xml")
LOCALES = [
    "values-ar", "values-de", "values-en", "values-es", "values-fr", "values-hi",
    "values-it", "values-ja", "values-ko", "values-nl", "values-pl", "values-pt",
    "values-ru", "values-sv", "values-th", "values-tr", "values-vi",
    "values-zh-rHK", "values-zh-rMO", "values-zh-rTW",
]

# Match single-line <string name="key">value</string>
STRING_LINE_RE = re.compile(r'^(\s*)<string name="([^"]+)">(.*)</string>\s*$')


def parse_strings_to_map(path):
    """Parse strings.xml and return dict name -> value (raw content between > and </string>)."""
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    result = {}
    # Match <string name="key">value</string> with value possibly containing newlines
    pattern = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.DOTALL)
    for m in pattern.finditer(text):
        result[m.group(1)] = m.group(2)
    return result


def escape_xml_value(s):
    """Escape for use inside XML text content. Only escape & not already part of entity."""
    s = re.sub(r"&(?!amp;|lt;|gt;|quot;)", "&amp;", s)
    return s.replace("<", "&lt;").replace(">", "&gt;")


def merge_file(locale_path, default_lines, default_map, locale_map):
    """Write merged strings.xml to locale_path. Use locale value if present else default."""
    out_lines = []
    if not default_lines[0].strip().startswith("<?xml"):
        out_lines.append('<?xml version="1.0" encoding="utf-8"?>')
    for line in default_lines:
        if line.strip().startswith("<?xml"):
            continue
        m = STRING_LINE_RE.match(line)
        if m:
            indent, key, default_val = m.group(1), m.group(2), m.group(3)
            use = locale_map.get(key, default_val)
            # Only escape when value came from locale (default_val is already valid XML from file)
            if key in locale_map:
                use = escape_xml_value(use)
            out_lines.append(f'{indent}<string name="{key}">{use}</string>')
        else:
            out_lines.append(line.rstrip("\n"))
    with open(locale_path, "w", encoding="utf-8") as f:
        f.write("\n".join(out_lines))
        f.write("\n")


def main():
    with open(VALUES_DEFAULT, "r", encoding="utf-8") as f:
        default_lines = f.readlines()
    default_map = parse_strings_to_map(VALUES_DEFAULT)
    for locale in LOCALES:
        locale_path = os.path.join(RES_DIR, locale, "strings.xml")
        if not os.path.isfile(locale_path):
            print("Skip (not found):", locale_path)
            continue
        locale_map = parse_strings_to_map(locale_path)
        merge_file(locale_path, default_lines, default_map, locale_map)
        print("Updated:", locale, "existing keys:", len(locale_map))


if __name__ == "__main__":
    main()
