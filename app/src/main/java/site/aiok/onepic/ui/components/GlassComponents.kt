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
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F7FA), // Light Cyan
                        Color(0xFFE1BEE7), // Light Purple
                        Color(0xFFF8BBD0)  // Light Pink
                    )
                )
            )
    ) {
        content()
    }
}

// Glass Container
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.4f))
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
