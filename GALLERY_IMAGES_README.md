# Gallery Levels 图片添加指南

## 📁 目录位置
```
app/src/main/assets/gallery_levels/
```

## 🖼️ 如何添加图片

### 方法1：直接复制文件
1. 将图片文件复制到 `app/src/main/assets/gallery_levels/` 目录
2. 支持的格式：`.png`、`.jpg`、`.jpeg`
3. 建议分辨率：至少 800x800 像素（越高越好）

### 方法2：通过 Android Studio
1. 在 Android Studio 中，右键点击 `app/src/main/assets/gallery_levels/` 目录
2. 选择 `New` → `File` 或 `Image Asset`
3. 将图片文件拖拽到该目录

## 📝 命名建议

### 推荐命名方案（按风格分类）

#### 方案1：按风格前缀命名
```
nature_01.png      # 自然风景
nature_02.png
architecture_01.png # 建筑
architecture_02.png
animals_01.png     # 动物
animals_02.png
art_01.png         # 艺术作品
art_02.png
food_01.png        # 美食
food_02.png
```

#### 方案2：按数字顺序命名
```
01.png
02.png
03.png
...
50.png
```

#### 方案3：混合命名（推荐）
```
01_nature.png
02_architecture.png
03_animals.png
04_art.png
05_food.png
...
```

## 🎨 图片风格建议

为了游戏体验的多样性，建议添加以下风格的图片：

1. **自然风景** 🌲
   - 山川、湖泊、森林、海滩
   - 日出、日落、星空

2. **建筑** 🏛️
   - 历史建筑、现代建筑
   - 地标建筑、城市景观

3. **动物** 🐾
   - 野生动物、宠物
   - 鸟类、海洋生物

4. **艺术** 🎨
   - 绘画、雕塑
   - 抽象艺术、插画

5. **美食** 🍕
   - 精致料理、甜点
   - 特色美食

6. **其他** ✨
   - 科技、太空
   - 节日、文化

## 📊 图片分配逻辑

- 系统会自动从 `gallery_levels` 目录读取所有图片
- 如果图片少于50张，会智能循环使用（使用偏移算法避免重复）
- 图片按文件名排序后分配
- 每个关卡会根据难度自动设置网格大小（3x3 到 6x8）

## ⚠️ 注意事项

1. **文件大小**：建议每张图片不超过 2MB，以保证加载速度
2. **图片格式**：优先使用 PNG（支持透明）或 JPG（文件更小）
3. **命名规范**：避免使用特殊字符，建议使用英文和数字
4. **图片质量**：确保图片清晰，避免模糊或低分辨率图片
5. **版权**：确保使用的图片有合法授权

## 🔄 更新图片后

添加新图片后，需要：
1. 重新编译项目（Build → Rebuild Project）
2. 或者直接运行应用，系统会自动检测新图片

## 📦 示例文件结构

```
app/src/main/assets/gallery_levels/
├── 01_nature.png
├── 02_architecture.png
├── 03_animals.png
├── 04_art.png
├── 05_food.png
├── 06_nature.png
├── 07_architecture.png
└── ... (更多图片)
```

## 🎯 快速开始

1. 准备至少 5-10 张不同风格的图片
2. 将图片复制到 `app/src/main/assets/gallery_levels/` 目录
3. 使用推荐的命名方案（如 `01_nature.png`）
4. 重新编译并运行应用
5. 在关卡选择界面查看效果



