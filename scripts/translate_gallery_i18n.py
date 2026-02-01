#!/usr/bin/env python3
"""
将 gallery_levels/i18n 下各语言 JSON 中仍为英文/简体中文的 description、story_text
翻译为目标语言。需安装: pip install deep-translator
"""
import json
import os
import sys
import time
from copy import deepcopy
from typing import Optional

def log(msg: str) -> None:
    print(msg, flush=True)

try:
    from deep_translator import GoogleTranslator
except ImportError:
    log("请先安装: pip install deep-translator")
    raise

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
ZH_REF_LOCALES = {"zh-rHK", "zh-rMO", "zh-rTW"}

# 文件名(无.json) -> Google 目标语言码
LOCALE_TO_TARGET = {
    "ar": "ar",
    "de": "de",
    "es": "es",
    "fr": "fr",
    "hi": "hi",
    "it": "it",
    "ja": "ja",
    "ko": "ko",
    "nl": "nl",
    "pl": "pl",
    "pt": "pt",
    "ru": "ru",
    "sv": "sv",
    "th": "th",
    "tr": "tr",
    "vi": "vi",
    "zh-rHK": "zh-TW",
    "zh-rMO": "zh-TW",
    "zh-rTW": "zh-TW",
}


def load_json(path: str) -> list:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(path: str, data: list) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def translate_batch(translator, texts: list, label: str = "") -> list:
    """分批翻译，每批最多 20 条，避免 API 限制。"""
    out = []
    batch_size = 20
    total_batches = (len(texts) + batch_size - 1) // batch_size
    for b in range(0, len(texts), batch_size):
        batch_num = b // batch_size + 1
        log(f"    {label} 批次 {batch_num}/{total_batches} ({min(b + batch_size, len(texts))}/{len(texts)} 条)...")
        batch = [t[:5000] if t and t.strip() else "" for t in texts[b : b + batch_size]]
        try:
            results = translator.translate_batch(batch)
            out.extend(results or batch)
        except Exception as e:
            log(f"    [batch error] {e!r}, 逐条重试...")
            for t in batch:
                try:
                    r = translator.translate(t) if t else t
                    out.append(r or t)
                except Exception as e2:
                    out.append(t)
                    log(f"      skip: {t[:40]}...")
                time.sleep(0.15)
        time.sleep(0.3)
    return out


def main(only_locale: Optional[str] = None) -> None:
    i18n_dir = os.path.abspath(I18N_DIR)
    en_path = os.path.join(i18n_dir, REF_EN)
    zh_path = os.path.join(i18n_dir, REF_ZH)

    en_list = load_json(en_path)
    zh_list = load_json(zh_path)
    ref_by_id_en = {e["id"]: e for e in en_list}
    ref_by_id_zh = {e["id"]: e for e in zh_list}

    json_files = [
        f
        for f in os.listdir(i18n_dir)
        if f.endswith(".json") and f not in (REF_EN, REF_ZH)
    ]
    if only_locale:
        json_files = [f for f in json_files if f.replace(".json", "") == only_locale]
        if not json_files:
            log(f"未找到语言: {only_locale}")
            return

    log(f"开始翻译，共 {len(json_files)} 个语言文件")
    for filename in sorted(json_files):
        locale_name = filename.replace(".json", "")
        target = LOCALE_TO_TARGET.get(locale_name)
        if not target:
            log(f"跳过（无目标码）: {filename}")
            continue

        use_zh = locale_name in ZH_REF_LOCALES
        ref_by_id = ref_by_id_zh if use_zh else ref_by_id_en
        source = "zh-CN" if use_zh else "en"

        path = os.path.join(i18n_dir, filename)
        locale_list = load_json(path)
        need_desc = []
        need_story = []
        indices_desc = []
        indices_story = []

        for i, entry in enumerate(locale_list):
            eid = entry["id"]
            ref = ref_by_id.get(eid)
            if not ref:
                continue
            if entry.get("description") == ref.get("description"):
                need_desc.append(ref["description"])
                indices_desc.append((i, "description"))
            if entry.get("story_text") == ref.get("story_text"):
                need_story.append(ref["story_text"])
                indices_story.append((i, "story_text"))

        total = len(need_desc) + len(need_story)
        if total == 0:
            log(f"{filename}: 已全部翻译，跳过")
            continue

        log(f"{filename}: 需翻译 {total} 条 (description={len(need_desc)}, story_text={len(need_story)})")
        translator = GoogleTranslator(source=source, target=target)

        translated_desc = translate_batch(translator, need_desc, label="description")
        translated_story = translate_batch(translator, need_story, label="story_text")

        for (idx, key), val in zip(indices_desc, translated_desc):
            locale_list[idx][key] = val
        for (idx, key), val in zip(indices_story, translated_story):
            locale_list[idx][key] = val

        save_json(path, locale_list)
        log(f"  已写入 {filename}")

    log("全部完成")


if __name__ == "__main__":
    only_locale = sys.argv[1] if len(sys.argv) > 1 else None
    main(only_locale=only_locale)
