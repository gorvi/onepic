package site.aiok.onepic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Premium Mesh Gradient Background
@Composable
fun MeshGradientBackground(
    colors: List<Color> = listOf(
        Color(0xFF1A237E), // 深蓝色中心
        Color(0xFF311B92), // 深紫色
        Color(0xFF0D47A1), // 普鲁士蓝
        Color(0xFF000000)  // 黑色边缘
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = colors,
                    center = androidx.compose.ui.geometry.Offset(0.2f, 0.2f),
                    radius = 2000f
                )
            )
    ) {
        // 添加一层柔和的极光感渐变覆盖
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                            Color.White.copy(alpha = 0.05f), // 极光亮部
                            Color.Transparent, 
                            Color.Black.copy(alpha = 0.6f)   // 深邃底部
                    )
                )
            )
        )
        content()
    }
}

// Glass Container
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.4f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

// Glass Top Bar
@Composable
fun GlassTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (showBackground) {
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color(0xFF00B0FF).copy(alpha = 0.3f), // Cyan neon accent
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (showBackground) {
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF00B0FF).copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            // Balance the row
            if (showBackground) {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        
        // Cyber-corner decoration
        if (showBackground) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(10.dp, 2.dp)
                    .background(Color(0xFF00B0FF).copy(alpha = 0.6f))
            )
        }
    }
}
