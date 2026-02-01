# 《星际拼图》(Starry Puzzle) 项目资源汇总

此文档汇总了项目在发布前准备的关键资源路径及说明。

---

## 🎨 视觉资源
- **官网目录**: `/Users/ghw/AndroidStudioProjects/2026/onepic/onepic512/`
- **官网主页**: [index.html](file:///Users/ghw/AndroidStudioProjects/2026/onepic/onepic512/index.html)
- **核心图标**: `/Users/ghw/AndroidStudioProjects/2026/onepic/onepic512/rocket_square.png` (请记得上传至服务器作为 `icon.png`)

---

## 📈 ASO/SEO 资产

### 1. 应用介绍 (Store Description)

**中文标题:** 星际拼图 - 赛博朋克科幻益智解谜
**中文描述:** 《星际拼图》(OnePic: Starry Puzzle) 是一款融合了赛博朋克美学与太空探索主题的高级益智拼图游戏。在浩瀚星海中寻找失落核心，通过极具挑战性的脑力开发关卡，重构人类文明蓝图。专为热爱科幻、拼图挑战和逻辑解谜的玩家打造。

**English Title:** Starry Puzzle - Sci-Fi Jigsaw & Brain Teaser
**English Description:** Starry Puzzle is a premium jigsaw puzzle game set in a sci-fi universe. Unlock the secrets of the galaxy, train your brain with challenging puzzles, and enjoy a relaxing space odyssey. Perfect for fans of strategy games, logic puzzles, and cosmic adventures.

### 2. 新版本说明 (What's New - v1.0.0)

**中文:** 
- 【正式启航】首个版本发布，内含十二大章节。
- 【每日同步】每日打卡领奖励，加速方舟建设。
- 【性能极致】开启混淆优化，包体极小。

**English:**
- [Launch!] Initial release with 12 unique chapters.
- [Daily Rewards] Check-in daily for coin bonuses.
- [Optimized] Lightweight build with R8 obfuscation.

---

## 🛠️ 技术配置状态
- **代码混淆**: 已开启 (`isMinifyEnabled = true`)
- **资源压缩**: 已开启 (`isShrinkResources = true`)
- **全语种名称本地化**: 已根据全球 18 种语言习惯深度定制（如：ja-星空のパズル, ko-별빛 퍼즐等），实现最大化 ASO。
- **包名**: `site.aiok.onepic`
- **Deep Link**: 支持 `netrill.com/onepic` 链接直达 App。

---

## 🚀 部署建议
1. 将 `onepic512` 整个目录的内容上传到您的服务器 `netrill.com/onepic/` 路径下。
2. 确保 `index.html` 中的 Logo 路径正确。
3. 应用上架 Google Play 后，官网按钮将自动生效。
