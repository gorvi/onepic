package site.aiok.onepic.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import site.aiok.onepic.data.LevelProgressManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import site.aiok.onepic.R

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", R.string.nav_home, Icons.Default.Home)
    object Galaxy : BottomNavItem("galaxy", R.string.nav_galaxy, Icons.Default.Star) // 银河 Tab
    object CheckIn : BottomNavItem("checkin", R.string.nav_checkin, Icons.Default.CheckCircle)
    object More : BottomNavItem("more", R.string.nav_personal_center, Icons.Default.Person)
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Galaxy,
        BottomNavItem.CheckIn,
        BottomNavItem.More
    )
    
    NavigationBar(
        modifier = modifier,
        containerColor = Color(0xFF121212).copy(alpha = 0.95f),
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val context = LocalContext.current
            val showRedDot = item is BottomNavItem.CheckIn && LevelProgressManager.shouldShowCheckInRedDot(context)

            NavigationBarItem(
                icon = { 
                    Box {
                        Icon(item.icon, contentDescription = stringResource(item.titleResId))
                        if (showRedDot) {
                            // 小红点
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .border(1.dp, Color(0xFF121212), CircleShape)
                            )
                        }
                    }
                },
                label = { 
                    Text(
                        text = stringResource(item.titleResId), 
                        style = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    ) 
                },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f),
                    indicatorColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}

