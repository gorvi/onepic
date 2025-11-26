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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Premium Mesh Gradient Background
@Composable
fun MeshGradientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB39DDB), // 淡紫色中心
                        Color(0xFF90CAF9), // 淡蓝色
                        Color(0xFF80CBC4), // 青绿色
                        Color(0xFFCE93D8)  // 粉紫色边缘
                    ),
                    center = androidx.compose.ui.geometry.Offset(0.3f, 0.3f),
                    radius = 1500f
                )
            )
    ) {
        // 添加一层柔和的渐变覆盖
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                            Color(0xFFE1F5FE).copy(alpha = 0.3f), // 浅蓝
                            Color(0xFFF3E5F5).copy(alpha = 0.4f), // 浅紫
                            Color(0xFFFCE4EC).copy(alpha = 0.3f)  // 浅粉
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
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.DarkGray
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.DarkGray,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            // Balance the row
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}
