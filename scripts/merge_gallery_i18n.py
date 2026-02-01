#!/usr/bin/env python3
"""
补全 gallery_levels/i18n 下各语言 JSON：以 en.json 或 zh.json 为基准，
缺失的关卡条目用基准内容补全，已有翻译保留。
"""
import json
import os
from copy import deepcopy

I18N_DIR = os.path.join(
    os.path.dirname(__file__),
    "..",
    "app",
    "src",
    "main",
    "assets",
    "gallery_levels",
    "i18n",
)
REF_EN = "en.json"
REF_ZH = "zh.json"
# 使用中文为基准的 locale（繁体等）
ZH_REF_LOCALES = {"zh-rHK", "zh-rMO", "zh-rTW"}


def load_json(path: str) -> list:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(path: str, data: list) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def main() -> None:
    i18n_dir = os.path.abspath(I18N_DIR)
    en_path = os.path.join(i18n_dir, REF_EN)
    zh_path = os.path.join(i18n_dir, REF_ZH)

    en_list = load_json(en_path)
    zh_list = load_json(zh_path)

    ref_by_id_en = {e["id"]: deepcopy(e) for e in en_list}
    ref_by_id_zh = {e["id"]: deepcopy(e) for e in zh_list}

    all_ids = sorted(ref_by_id_en.keys())
    assert all_ids == list(range(1, 61)), "en.json 应为 id 1~60"

    json_files = [
        f
        for f in os.listdir(i18n_dir)
        if f.endswith(".json") and f not in (REF_EN, REF_ZH)
    ]

    for filename in sorted(json_files):
        locale_name = filename.replace(".json", "")
        use_zh = locale_name in ZH_REF_LOCALES
        ref_by_id = ref_by_id_zh if use_zh else ref_by_id_en
        ref_label = "zh.json" if use_zh else "en.json"

        path = os.path.join(i18n_dir, filename)
        locale_list = load_json(path)
        locale_by_id = {e["id"]: e for e in locale_list}

        merged = []
        for iid in all_ids:
            entry = deepcopy(ref_by_id[iid])
            if iid in locale_by_id:
                entry["description"] = locale_by_id[iid]["description"]
                entry["story_text"] = locale_by_id[iid]["story_text"]
            merged.append(entry)

        save_json(path, merged)
        filled = sum(1 for iid in all_ids if iid in locale_by_id)
        print(f"{filename}: 保留 {filled} 条翻译，缺项用 {ref_label} 补全，共 {len(merged)} 条")


if __name__ == "__main__":
    main()
