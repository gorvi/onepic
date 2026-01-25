# 如何自定义 Android 应用图标图片

## 🎨 方法一：使用 Android Studio Asset Studio（推荐，最简单）

### 步骤 1: 打开 Asset Studio

1. 在 Android Studio 中，右键点击 `res` 文件夹
2. 选择 **New > Image Asset**
3. 或者：**File > New > Image Asset**

### 步骤 2: 配置图标

#### 2.1 选择图标类型
- **Icon Type**: 选择 "Launcher Icons (Adaptive and Legacy)"
- **Name**: `ic_launcher`（保持默认）

#### 2.2 配置前景层（Foreground Layer）

1. 点击 **"Foreground Layer"** 标签
2. 在 **"Asset Type"** 中选择：
   - ✅ **Image** - 使用你自己的图片文件（推荐）
   - Clip art - 使用内置剪贴画
   - Text - 使用文字

3. 如果选择 **Image**：
   - 点击 **Path** 字段旁边的文件夹图标 📁
   - 浏览并选择你的图片文件
   - **推荐格式**: PNG（透明背景）或 SVG
   - **推荐尺寸**: 至少 512 x 512 像素，最好是 1024 x 1024

4. 调整设置：
   - **Trim**: 选择 "Yes" 自动裁剪空白边缘
   - **Resize**: 拖动滑块调整大小（建议 80-90%）

#### 2.3 配置背景层（Background Layer）

1. 点击 **"Background Layer"** 标签
2. 选择背景类型：
   - **Color**: 纯色背景（推荐）
     - 点击颜色选择器，选择你的品牌色
   - **Image**: 使用背景图片
   - **None**: 无背景（不推荐）

3. 如果选择 **Color**：
   - 建议使用与应用主题一致的颜色
   - 例如：`#6B46C1`（紫色）或 `#3B82F6`（蓝色）

#### 2.4 预览

- 右侧会实时显示图标在不同形状下的效果：
  - Circle（圆形）
  - Squircle（圆角矩形）
  - Rounded Square（圆角方形）
- ✅ 勾选 **"Show safe zone"** 查看安全区域
- 确保重要元素在安全区域内

#### 2.5 完成

1. 点击 **"Next"**
2. 检查生成的文件列表
3. 点击 **"Finish"**
4. Android Studio 会自动替换所有图标文件

---

## 🛠️ 方法二：手动替换 XML 文件（高级）

如果你想完全控制图标设计，可以手动编辑 XML 文件。

### 步骤 1: 准备图片

1. **前景图片**：
   - 格式：PNG（透明背景）或 SVG
   - 尺寸：1024 x 1024 像素
   - 内容：主要图标元素，放在中心

2. **背景**：
   - 可以是纯色或渐变
   - 尺寸：1024 x 1024 像素

### 步骤 2: 转换为 SVG

如果你的图片是 PNG，需要转换为 SVG：

**在线工具**：
- https://convertio.co/zh/png-svg/
- https://www.vectorizer.io/

### 步骤 3: 转换为 Android Vector Drawable

**方法 A: 使用在线工具**
- https://inloop.github.io/svg2android/
- 上传 SVG，自动生成 XML

**方法 B: 使用 Android Studio**
1. 右键点击 `drawable` 文件夹
2. **New > Vector Asset**
3. 选择 "Local file (SVG, PSD)"
4. 选择你的 SVG 文件
5. 生成 XML

### 步骤 4: 更新图标文件

#### 更新背景层 (`ic_launcher_background.xml`)

**纯色背景示例**：
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- 纯色背景 -->
    <path
        android:fillColor="#6B46C1"  <!-- 你的颜色 -->
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

**渐变背景示例**：
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor" xmlns:aapt="http://schemas.android.com/aapt">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:type="linear">
                <item android:offset="0" android:color="#6B46C1" />
                <item android:offset="1" android:color="#3B82F6" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
```

#### 更新前景层 (`ic_launcher_foreground.xml`)

将转换后的 SVG 内容粘贴到这里，确保：
- 重要元素在中心 72x72 dp 区域内
- 使用适当的颜色和透明度

### 步骤 5: 测试

1. 运行应用
2. 查看桌面图标
3. 检查不同设备上的显示效果

---

## 📝 快速开始：使用现有图片

### 如果你已经有一张图片：

1. **准备图片**：
   - 尺寸：1024 x 1024 像素
   - 格式：PNG（透明背景）
   - 内容：拼图元素或应用标识

2. **使用 Asset Studio**：
   - 打开 Asset Studio
   - 前景层：选择你的 PNG 图片
   - 背景层：选择纯色（如 `#6B46C1`）
   - 调整大小到 80-90%
   - 完成！

---

## 🎯 针对 OnePic 的建议

### 设计思路：

1. **前景层**：
   - 白色或浅色的拼图块
   - 中心有数字 "1" 或图片图标
   - 简洁、现代的设计

2. **背景层**：
   - 紫色到蓝色的渐变
   - 或纯色背景（如 `#6B46C1`）

### 示例颜色方案：

```xml
<!-- 背景：紫色渐变 -->
<gradient>
    <item android:offset="0" android:color="#6B46C1" />  <!-- 紫色 -->
    <item android:offset="1" android:color="#3B82F6" />  <!-- 蓝色 -->
</gradient>
```

---

## ⚠️ 注意事项

1. **安全区域**：
   - 重要内容放在中心 72x72 dp 区域
   - 边缘可能被裁剪

2. **图片质量**：
   - 使用高分辨率图片（至少 512x512）
   - 避免模糊或像素化

3. **透明度**：
   - 前景层可以有透明区域
   - 背景层必须完全填充

4. **颜色对比**：
   - 确保前景和背景有足够对比度
   - 在深色和浅色主题下都可见

5. **测试**：
   - 在不同设备上测试
   - 检查圆形、圆角矩形等不同形状下的显示

---

## 🔧 故障排除

### 问题：图标显示不正确
- 检查 XML 语法是否正确
- 确保路径正确
- 清理并重新构建项目：**Build > Clean Project**

### 问题：图标太大/太小
- 在 Asset Studio 中调整 Resize 滑块
- 或手动调整 SVG 的 viewport

### 问题：图标被裁剪
- 确保重要元素在安全区域内
- 使用 "Show safe zone" 预览

---

## 📚 相关资源

- [Android 自适应图标文档](https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive)
- [Material Design 图标指南](https://material.io/design/iconography/product-icons.html)
- [在线 SVG 转 Android Vector 工具](https://inloop.github.io/svg2android/)

---

需要我帮你创建一个示例图标吗？告诉我你的设计想法或颜色偏好！
