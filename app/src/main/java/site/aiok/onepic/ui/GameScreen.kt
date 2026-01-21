package site.aiok.onepic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onGloballyPositioned
import site.aiok.onepic.logic.ImageSlicer
import site.aiok.onepic.view.GameBoardView
import site.aiok.onepic.ui.components.GlassTopBar
import site.aiok.onepic.ui.components.MeshGradientBackground
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.model.ImageSource

@Composable
fun GameScreen(
    levelConfig: LevelConfig, 
    levelIndex: Int,
    levelMode: String,
    onBack: () -> Unit, 
    onLevelComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var elapsedTime by remember { mutableStateOf(0) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showPuzzleButtons by remember { mutableStateOf(false) }  // 控制拼图内的按钮显示
    var completionStars by remember { mutableStateOf(0) }
    var scoreGained by remember { mutableStateOf(0) }
    var currentScore by remember { mutableStateOf(0) }  // 实时得分（合并/拆解产生的分数）
    
    // 获取历史最佳时间
    val bestTime = remember(levelIndex, levelMode) {
        when (levelMode) {
            "classic" -> site.aiok.onepic.data.LevelProgressManager.getClassicLevelBestTime(context, levelIndex)
            "gallery" -> site.aiok.onepic.data.LevelProgressManager.getGalleryLevelBestTime(context, levelIndex)
            else -> Int.MAX_VALUE
        }
    }
    
    // 获取历史最佳星星数（用于显示）- 使用mutableStateOf以便实时更新
    var bestStars by remember(levelIndex, levelMode) {
        mutableStateOf(
            when (levelMode) {
                "classic" -> site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, levelIndex)
                "gallery" -> site.aiok.onepic.data.LevelProgressManager.getGalleryLevelStars(context, levelIndex)
                else -> 0
            }
        )
    }
    
    // 星星点亮动画状态
    var animatedStarIndex by remember { mutableStateOf(-1) }
    var shouldAnimateStars by remember { mutableStateOf(false) }
    var newStarsCount by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    // 触发星星点亮动画
    LaunchedEffect(shouldAnimateStars, newStarsCount) {
        if (shouldAnimateStars && newStarsCount > bestStars) {
            val oldStars = bestStars
            for (i in oldStars until newStarsCount) {
                kotlinx.coroutines.delay((i - oldStars) * 300L)
                animatedStarIndex = i
                kotlinx.coroutines.delay(500L)
                animatedStarIndex = -1
            }
            bestStars = newStarsCount
            shouldAnimateStars = false
        }
    }
    
    // Load bitmap based on ImageSource
    val bitmap = remember(levelConfig) {
        try {
            val originalBitmap = when (val source = levelConfig.imageSource) {
                is ImageSource.Generated -> {
                    null // Will be generated below
                }
                is ImageSource.Asset -> {
                    val inputStream = context.assets.open(source.path)
                    BitmapFactory.decodeStream(inputStream)
                }
                is ImageSource.UriSource -> {
                    if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, source.uri)
                    } else {
                        val sourceDec = ImageDecoder.createSource(context.contentResolver, source.uri)
                        ImageDecoder.decodeBitmap(sourceDec) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    }
                }
                is ImageSource.Resource -> {
                    BitmapFactory.decodeResource(context.resources, source.resId)
                }
            }

            // Standardize Bitmap Size
            if (originalBitmap != null) {
                // Scale down if too large, or fit to a standard game size
                // For now, we scale to a max dimension to keep performance good
                val maxDim = 1024
                val scale = if (originalBitmap.width > maxDim || originalBitmap.height > maxDim) {
                    minOf(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height)
                } else {
                    1f
                }
                
                if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * scale).toInt(),
                        (originalBitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    originalBitmap
                }
            } else {
                 // Generated Fallback (Classic Mode)
                 // 第S关（c1）使用竖向图片（600x800），其他关卡使用横向图片（800x600）
                 val isVertical = levelConfig.levelId == "c1"
                 val width = if (isVertical) 600 else 800
                 val height = if (isVertical) 800 else 600
                 val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
                 val canvas = android.graphics.Canvas(bmp)
                 val paint = android.graphics.Paint()
        
                 // Use hash of ID for consistent generation
                 val seed = levelConfig.levelId.hashCode().toLong()
                 
                 // Base Hue based on level
                 val baseHue = (seed * 77) % 360f
                 
                 // 1. Background Gradient (Diagonal)
                 val colors = intArrayOf(
                     android.graphics.Color.HSVToColor(floatArrayOf(baseHue, 0.3f, 0.9f)),
                     android.graphics.Color.HSVToColor(floatArrayOf((baseHue + 40) % 360f, 0.4f, 0.8f))
                 )
                 val gradient = android.graphics.LinearGradient(
                     0f, 0f, width.toFloat(), height.toFloat(),
                     colors, null, android.graphics.Shader.TileMode.CLAMP
                 )
                 paint.shader = gradient
                 canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                 paint.shader = null // Reset shader
                 
                 // 2. Grid Lines (Subtle help for alignment)
                 paint.strokeWidth = 2f
                 paint.color = android.graphics.Color.argb(50, 0, 0, 0)
                 for (i in 0..width step 100) {
                     canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
                 }
                 for (i in 0..height step 100) {
                     canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
                 }
                 
                 // 3. Random Geometric Shapes (to create unique edges)
                 val rnd = java.util.Random(seed)
                 for (i in 0..15) {
                     paint.color = android.graphics.Color.HSVToColor(
                         200, // Alpha
                         floatArrayOf((baseHue + rnd.nextInt(180)) % 360f, 0.7f, 0.8f)
                     )
                     
                     val cx = rnd.nextFloat() * width
                     val cy = rnd.nextFloat() * height
                     val size = 50f + rnd.nextFloat() * 150f
                     
                     when (rnd.nextInt(3)) {
                         0 -> canvas.drawCircle(cx, cy, size / 2, paint)
                         1 -> canvas.drawRect(cx - size/2, cy - size/2, cx + size/2, cy + size/2, paint)
                         2 -> {
                             // Triangle
                             val path = android.graphics.Path()
                             path.moveTo(cx, cy - size/2)
                             path.lineTo(cx - size/2, cy + size/2)
                             path.lineTo(cx + size/2, cy + size/2)
                             path.close()
                             canvas.drawPath(path, paint)
                         }
                     }
                 }
                 
                 // 4. Large Text Number (Center)
                 paint.color = android.graphics.Color.argb(100, 255, 255, 255)
                 paint.textSize = 400f
                 paint.textAlign = android.graphics.Paint.Align.CENTER
                 paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                 // Center text vertically
                 val textBounds = android.graphics.Rect()
                 // 第一关（c1）显示"S"，其他关卡显示数字
                 val idText = if (levelConfig.levelId == "c1") {
                     "S"
                 } else {
                     levelConfig.levelId.replace("c", "").replace("g_", "") // Simple display for classic/gallery
                 }
                 paint.getTextBounds(idText, 0, idText.length, textBounds)
                 val yOffset = textBounds.height() / 2f
                 canvas.drawText(idText, width / 2f, height / 2f + yOffset, paint)

                 // 5. Border
                 paint.style = android.graphics.Paint.Style.STROKE
                 paint.strokeWidth = 10f
                 paint.color = android.graphics.Color.BLACK
                 canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
                 bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to empty bitmap - 第S关使用竖向，其他使用横向
            val isVertical = levelConfig.levelId == "c1"
            val fallbackWidth = if (isVertical) 600 else 800
            val fallbackHeight = if (isVertical) 800 else 600
            Bitmap.createBitmap(fallbackWidth, fallbackHeight, Bitmap.Config.ARGB_8888)
        }
    }

    val pieces = remember(levelConfig) {
        ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols)
    }
    
    // 存储图片的实际渲染高度，用于调整白色边框高度
    var imageHeight by remember { mutableStateOf<androidx.compose.ui.unit.Dp?>(null) }

    // 计时器更新 - 使用remember缓存格式化时间，优化性能
    var gameBoardView: GameBoardView? by remember { mutableStateOf(null) }
    val formattedTime = remember(elapsedTime) {
        site.aiok.onepic.ui.components.formatTime(elapsedTime)
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            gameBoardView?.let {
                elapsedTime = it.getElapsedSeconds()
            }
        }
    }

    MeshGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            // 关卡标题：第1关（c1）显示"Level S"，第2关开始显示"Level 1", "Level 2"...
            val displayTitle = if (levelConfig.levelId == "c1") {
                "Level S"
            } else {
                // 第2关（index=1）开始，显示为Level 1, Level 2...
                "Level ${levelIndex}"
            }
            GlassTopBar(
                title = displayTitle,
                onBack = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            // 计时器和星星显示 - 添加层叠效果（一层嵌一层）
            // 增加高度，方便后续增加其他按钮
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // 第一排按钮：计时器、分数、星星
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),  // 增加垂直padding，增加这一排的高度
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // 计时器 - 优化布局防止换行，添加层叠效果（最底层）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = 0.dp, y = 0.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.15f)
                        )
                ) {
                    site.aiok.onepic.ui.components.GlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⏱",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF1565C0),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // 实时得分（使用硬币emoji）- 添加层叠效果（中间层）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-2).dp, y = 2.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                ) {
                    site.aiok.onepic.ui.components.GlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // 硬币emoji - 添加描边效果
                            Text(
                                text = "🪙",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 20.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                ),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = currentScore.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color(0xFFFFD700),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // 目标星星 - 显示历史最佳星星数，已获得显示黄色，未获得显示灰色，添加层叠效果和动画（最上层）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-4).dp, y = 4.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.25f)
                        )
                ) {
                    site.aiok.onepic.ui.components.GlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { index ->
                                val isEarned = index < bestStars
                                val scale by animateFloatAsState(
                                    targetValue = if (animatedStarIndex == index) 1.15f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "star_scale_$index"
                                )
                                
                                // 彻底重构：完全统一的结构，确保三个星星完全一样大
                                Box(
                                    modifier = Modifier
                                        .size(28.dp),  // 固定容器大小，所有星星完全一致
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 所有星星使用完全相同的结构
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .scale(scale),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 阴影层（所有星星都有，统一大小）
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (isEarned) {
                                                Color.Black.copy(alpha = 0.25f)
                                            } else {
                                                Color.Black.copy(alpha = 0.1f)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .offset(x = 1.5.dp, y = 1.5.dp)
                                        )
                                        
                                        // 描边层（所有星星都有，统一大小）
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        
                                        // 主体层（所有星星都有，统一大小）
                                        Box(
                                            modifier = Modifier.size(26.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // 主体颜色层（所有星星都有，统一大小）
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isEarned) {
                                                    Color(0xFFFFD700) // 金色
                                                } else {
                                                    Color(0xFFCCCCCC) // 灰色
                                                },
                                                modifier = Modifier.size(26.dp)
                                            )
                                            // 高光层（所有星星都有，统一大小）
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isEarned) {
                                                    Color(0xFFFFF59D).copy(alpha = 0.7f) // 浅金色高光
                                                } else {
                                                    Color(0xFFE0E0E0).copy(alpha = 0.5f) // 浅灰色高光
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                // 缩短间隔：从8.dp改为4.dp
                                if (index < 2) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
                }
                
                // 预留空间：后续可以在这里添加第二排按钮
                // 示例：
                // Spacer(modifier = Modifier.height(8.dp))  // 如果需要间距，可以添加
                // Row(
                //     modifier = Modifier
                //         .fillMaxWidth()
                //         .padding(vertical = 8.dp),
                //     horizontalArrangement = Arrangement.spacedBy(4.dp),
                //     verticalAlignment = Alignment.CenterVertically
                // ) {
                //     // 在这里添加新的按钮
                // }
            }
            
            // Game Area - 添加调试边框
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)  // 只保留左右padding，移除上下padding
            ) {
                // 拼图区域 - 只保留白色边框，去掉背景色
                val puzzleShape = remember { RoundedCornerShape(24.dp) }
                
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                        .padding(horizontal = 8.dp)  // 只保留左右padding，用于白色边框的外边距
                ) {
                    // 白色边框紧贴图片框，放在最内层
                    // 图片向下移动：添加顶部padding
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()  // 填满宽度
                            .padding(top = 16.dp)  // 向下移动16dp
                            .then(
                                if (imageHeight != null) {
                                    Modifier.height(imageHeight!!)  // 使用图片实际高度
                                } else {
                                    Modifier.fillMaxHeight()  // 初始时填满高度
                                }
                            )
                            .border(width = 2.dp, color = Color.White, shape = puzzleShape)  // 白色边框（实际边框），紧贴图片
                            .clip(puzzleShape),
                        contentAlignment = androidx.compose.ui.Alignment.TopCenter  // 顶部对齐，让图片从顶部开始
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    GameBoardView(ctx).apply {
                                        setPieces(pieces)
                                        gameBoardView = this
                                        
                                        // 处理实时得分变化（合并/拆解）
                                        onScoreChange = { scoreDelta ->
                                            currentScore += scoreDelta
                                            // 实时保存合并得分（scoreDelta可以是正数或负数）
                                            site.aiok.onepic.data.LevelProgressManager.saveTotalMergeScore(context, scoreDelta)
                                        }
                                        
                                        onPuzzleComplete = { timeInSeconds ->
                                            elapsedTime = timeInSeconds
                                            
                                            // 计算星星数（考虑历史最佳）
                                            completionStars = site.aiok.onepic.ui.components.calculateStars(
                                                timeInSeconds, 
                                                levelConfig.rows, 
                                                levelConfig.cols,
                                                bestTime
                                            )
                                            
                                            // 保存最佳时间和星星数
                                            when (levelMode) {
                                                "classic" -> {
                                                    site.aiok.onepic.data.LevelProgressManager.saveClassicLevelBestTime(context, levelIndex, timeInSeconds)
                                                    val currentBestStars = site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, levelIndex)
                                                    
                                                    // 更新逻辑：只保存最佳星星数（如果本次更好才更新）
                                                    if (completionStars > currentBestStars) {
                                                        // 计算星星数变化（新增的星星数）
                                                        val starsDelta = completionStars - currentBestStars
                                                        site.aiok.onepic.data.LevelProgressManager.saveClassicLevelStars(context, levelIndex, completionStars)
                                                        // 更新总星星数
                                                        site.aiok.onepic.data.LevelProgressManager.saveTotalStars(context, starsDelta)
                                                        // 更新显示的星星数
                                                        bestStars = completionStars
                                                        // 触发星星点亮动画（只动画新获得的星星）
                                                        newStarsCount = completionStars
                                                        shouldAnimateStars = true
                                                    } else {
                                                        // 即使本次星星数不如之前，也要更新显示（显示历史最佳）
                                                        bestStars = currentBestStars
                                                    }
                                                }
                                                "gallery" -> {
                                                    site.aiok.onepic.data.LevelProgressManager.saveGalleryLevelBestTime(context, levelIndex, timeInSeconds)
                                                    val currentBestStars = site.aiok.onepic.data.LevelProgressManager.getGalleryLevelStars(context, levelIndex)
                                                    
                                                    // 更新逻辑：只保存最佳星星数（如果本次更好才更新）
                                                    if (completionStars > currentBestStars) {
                                                        // 计算星星数变化（新增的星星数）
                                                        val starsDelta = completionStars - currentBestStars
                                                        site.aiok.onepic.data.LevelProgressManager.saveGalleryLevelStars(context, levelIndex, completionStars)
                                                        // 更新总星星数
                                                        site.aiok.onepic.data.LevelProgressManager.saveTotalStars(context, starsDelta)
                                                        // 更新显示的星星数
                                                        bestStars = completionStars
                                                        // 触发星星点亮动画（只动画新获得的星星）
                                                        newStarsCount = completionStars
                                                        shouldAnimateStars = true
                                                    } else {
                                                        // 即使本次星星数不如之前，也要更新显示（显示历史最佳）
                                                        bestStars = currentBestStars
                                                    }
                                                }
                                            }
                                            
                                            // scoreGained 应该是这局获得的合并得分（硬币分数），而不是星星数
                                            scoreGained = currentScore
                                            
                                            showCompleteDialog = true
                                            showPuzzleButtons = true  // 显示拼图内的按钮
                                            onLevelComplete()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()  // 填满整个区域
                                    .onGloballyPositioned { coordinates ->
                                        // 从GameBoardView获取图片的实际渲染高度
                                        gameBoardView?.let { view ->
                                            val actualHeightPx = view.actualImageHeight
                                            if (actualHeightPx > 0f) {
                                                val actualHeight = with(density) {
                                                    actualHeightPx.toDp()
                                                }
                                                if (imageHeight != actualHeight) {
                                                    imageHeight = actualHeight
                                                }
                                            }
                                        }
                                    }
                                    // 移除padding，让图片完全填满
                            )
                        }
                    }
                }
                
                // 按钮区域（在拼图Box外部，白色边框下方）
                if (showPuzzleButtons) {
                    val buttonSlideInOffset = remember { Animatable(200f) }
                    val buttonAlpha = remember { Animatable(0f) }
                    
                    LaunchedEffect(showPuzzleButtons) {
                        if (showPuzzleButtons) {
                            kotlinx.coroutines.delay(2000) // 延迟2秒显示按钮
                            buttonSlideInOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            buttonAlpha.animateTo(1f, animationSpec = tween(400))
                        }
                    }
                    
                    // 使用Box包装按钮，以便精确控制位置和对齐
                    // 按钮有offset(-96.dp)，所以需要在底部添加96.dp的空间，让红色边框下边缘对齐到按钮下边缘
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = buttonSlideInOffset.value.dp - 96.dp) // 往上移动3倍（从-32改为-96），更靠近拼图
                                .alpha(buttonAlpha.value)
                                .padding(top = 2.dp), // 距离拼图Box下边缘2dp，几乎紧贴
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 再来一次按钮
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = 0.2f),
                                        spotColor = Color.Black.copy(alpha = 0.3f)
                                    )
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFFFFF).copy(alpha = 0.95f),
                                                Color(0xFFF5F5F5).copy(alpha = 0.90f)
                                            ),
                                            center = androidx.compose.ui.geometry.Offset(0.3f, 0.3f)
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        showCompleteDialog = false
                                        showPuzzleButtons = false
                                        scoreGained = 0
                                        currentScore = 0  // 重置实时得分
                                        gameBoardView?.setPieces(
                                            ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols)
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "再来一次",
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(20.dp))
                            
                            // 下一步按钮
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFFFF6B35).copy(alpha = 0.3f),
                                        spotColor = Color(0xFFFF6B35).copy(alpha = 0.4f)
                                    )
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFF6B35),
                                                Color(0xFFFF8A50),
                                                Color(0xFFFFA366)
                                            ),
                                            center = androidx.compose.ui.geometry.Offset(0.3f, 0.3f)
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        showCompleteDialog = false
                                        showPuzzleButtons = false
                                        scoreGained = 0
                                        onBack() // 返回关卡选择界面，解锁机制会自动生效
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "下一步",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        
                        // 在按钮下方添加空间，让红色边框下边缘对齐到按钮下边缘
                        // 按钮有offset(-96.dp)，padding top 2.dp，按钮高度64.dp
                        // 按钮下边缘位置：-96.dp + 2.dp + 64.dp = -30.dp（相对于Box顶部）
                        // 所以需要添加30.dp的空间，让红色边框下边缘对齐到按钮下边缘
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)  // 高度等于按钮下边缘到Box顶部的距离，确保红色边框下边缘对齐
                        )
                    }
                }
            }
        }
        
        // 完成弹窗（在Box内部，Column外部）
        if (showCompleteDialog) {
            site.aiok.onepic.ui.components.LevelCompleteDialog(
                stars = completionStars,
                timeInSeconds = elapsedTime,
                scoreGained = scoreGained,
                onDismiss = {
                    showCompleteDialog = false
                    scoreGained = 0
                }
            )
        }
    }
    }
}
