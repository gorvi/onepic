# Android 应用图标设计指南

## 📱 当前项目图标结构

你的项目使用的是 **Android 自适应图标（Adaptive Icon）**，这是 Android 8.0 (API 26) 及以上版本的标准图标格式。

### 当前图标文件结构：
```
app/src/main/res/
├── mipmap-anydpi/
│   ├── ic_launcher.xml          # 自适应图标配置
│   └── ic_launcher_round.xml    # 圆形自适应图标配置
├── drawable/
│   ├── ic_launcher_background.xml   # 背景层（108x108dp）
│   └── ic_launcher_foreground.xml   # 前景层（108x108dp）
└── mipmap-*/                     # 各密度位图图标（已弃用，但保留兼容性）
    └── ic_launcher.webp
```

---

## 🎨 自适应图标设计要求

### 1. **尺寸规范**

#### 设计尺寸（推荐）
- **设计画布**: 1024 x 1024 像素（或更高）
- **安全区域**: 中心 66% 区域（约 675 x 675 像素）
- **实际显示**: 系统会根据设备形状裁剪（圆形、圆角矩形、方形等）

#### XML 资源尺寸
- **背景层**: 108 x 108 dp
- **前景层**: 108 x 108 dp
- **安全区域**: 72 x 72 dp（中心区域，不会被裁剪）

### 2. **安全区域（Safe Zone）**

```
┌─────────────────────────┐
│                         │
│   ┌─────────────────┐   │  ← 安全区域外可能被裁剪
│   │                 │   │
│   │   安全区域      │   │  ← 重要内容放在这里
│   │  (72x72 dp)     │   │
│   │                 │   │
│   └─────────────────┘   │
│                         │
└─────────────────────────┘
     108 x 108 dp
```

**重要提示**：
- ✅ 重要图标元素应放在中心 72x72 dp 区域内
- ❌ 避免在边缘放置关键信息（可能被裁剪）
- ✅ 背景可以延伸到整个 108x108 dp 区域

### 3. **图层结构**

自适应图标由两层组成：

#### 背景层（Background）
- **用途**: 纯色、渐变或简单图案
- **要求**: 
  - 可以是纯色（推荐）
  - 可以是渐变
  - 可以是简单纹理
  - **不能包含透明区域**（必须完全填充）

#### 前景层（Foreground）
- **用途**: 主要图标内容
- **要求**:
  - 包含应用的主要视觉元素
  - 可以包含透明区域
  - 重要内容应在安全区域内

### 4. **设计规范**

#### 视觉要求
- ✅ **简洁明了**: 在小尺寸下清晰可辨
- ✅ **高对比度**: 确保在各种背景下可见
- ✅ **品牌一致性**: 与应用内设计风格一致
- ✅ **无文字**: 避免在图标中使用文字（不同语言环境）

#### 颜色要求
- ✅ 使用鲜明、饱和的颜色
- ✅ 考虑深色/浅色主题兼容性
- ✅ 避免过于复杂的渐变

#### 形状要求
- ✅ 设计时考虑圆形、圆角矩形、方形等不同显示形状
- ✅ 确保在所有形状下都美观

---

## 🛠️ 设计工具推荐

### 1. **在线工具**
- **Android Asset Studio** (推荐)
  - 网址: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
  - 功能: 自动生成所有尺寸和格式的图标
  - 支持: 上传图片自动生成自适应图标

- **Icon Kitchen** (Google 官方)
  - 网址: https://icon.kitchen/
  - 功能: Google 官方图标生成工具
  - 特点: 支持预览不同设备上的显示效果

### 2. **设计软件**
- **Figma** (免费，推荐)
  - 有 Android 图标设计模板
  - 支持导出 SVG/PNG
  
- **Adobe Illustrator**
  - 专业矢量设计工具
  - 可导出为 SVG

- **Sketch** (Mac)
  - 专业 UI 设计工具

### 3. **AI 生成工具**
- **Midjourney / DALL-E**
  - 可以生成图标概念
  - 需要后期处理

---

## 📐 设计步骤

### 步骤 1: 准备设计文件
1. 创建 1024 x 1024 像素的画布
2. 标记安全区域（中心 675 x 675 像素）
3. 设计背景层和前景层

### 步骤 2: 设计背景层
```xml
<!-- 示例：纯色背景 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FF6B9E"  <!-- 你的品牌色 -->
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

### 步骤 3: 设计前景层
- 将主要图标元素放在中心
- 确保在安全区域内
- 使用 SVG 矢量格式（可缩放）

### 步骤 4: 导出和转换
1. 导出为 SVG 格式
2. 使用 Android Asset Studio 转换为 XML
3. 或手动编写 XML 代码

---

## 🎯 OnePic 图标设计建议

基于你的拼图游戏应用，建议：

### 设计概念
1. **拼图元素**: 使用拼图块作为主要视觉元素
2. **数字 "1"**: 结合 "OnePic" 中的 "1"
3. **图片框架**: 暗示图片拼图的概念
4. **渐变背景**: 使用现代、吸引人的渐变

### 颜色建议
- **主色**: 鲜艳的蓝色或紫色渐变
- **前景**: 白色或浅色的拼图元素
- **背景**: 深色或渐变背景

### 示例设计思路
```
背景层: 紫色到蓝色的渐变 (#6B46C1 → #3B82F6)
前景层: 白色拼图块，中心有数字 "1" 或图片图标
```

---

## 📦 如何更新图标

### 方法 1: 使用 Android Asset Studio（推荐）

1. 访问: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. 上传你的图标设计（1024x1024 PNG）
3. 调整裁剪区域
4. 下载生成的资源文件
5. 替换项目中的图标文件

### 方法 2: 手动创建 XML

1. 设计背景层和前景层
2. 转换为 SVG
3. 使用工具转换为 Android Vector Drawable
4. 更新 `ic_launcher_background.xml` 和 `ic_launcher_foreground.xml`

### 方法 3: 使用 Figma 模板

1. 搜索 "Android Adaptive Icon" Figma 模板
2. 在模板中设计
3. 导出为 SVG
4. 转换为 XML

---

## ✅ 检查清单

设计完成后，检查：

- [ ] 图标在 48dp 尺寸下清晰可辨
- [ ] 重要元素在安全区域内
- [ ] 背景层无透明区域
- [ ] 在不同形状下都美观（圆形、圆角矩形）
- [ ] 颜色对比度足够
- [ ] 符合 Material Design 规范
- [ ] 在深色和浅色背景下都可见

---

## 🔗 参考资源

- **Material Design 图标指南**: https://material.io/design/iconography/product-icons.html
- **Android 自适应图标文档**: https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive
- **图标设计最佳实践**: https://developer.android.com/guide/practices/ui_guidelines/icon_design

---

## 💡 快速开始模板

如果你想快速创建一个简单的图标，我可以帮你：

1. **生成一个简单的拼图主题图标**
2. **创建渐变背景**
3. **添加拼图块元素**

需要我帮你创建一个示例图标设计吗？
