package site.aiok.onepic.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import site.aiok.onepic.R
import site.aiok.onepic.data.LevelRepository
import site.aiok.onepic.data.LevelProgressManager
import site.aiok.onepic.model.ImageSource
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.components.GalaxyBackground
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(
    onLocateLevel: (Int) -> Unit = {},
    preloadedNativeAd: com.google.android.gms.ads.nativead.NativeAd? = null
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Memories, 1 = Blueprint
    val context = LocalContext.current
    
    var memories by remember { mutableStateOf<List<LevelConfig>>(emptyList()) }
    var completedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    val currentLanguage = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(context)
    
    LaunchedEffect(currentLanguage) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val rawMemories = LevelRepository.getGalleryLevels(context)
            // Sort: Interleave (1, 61, 2, 62...)
            val sortedMemories = rawMemories.sortedWith(compareBy<LevelConfig> { level ->
                 // Base ID: If > 60, subtract 60 to pair with original
                 val id = level.levelId.filter { it.isDigit() }.toIntOrNull() ?: -1
                 if (id > 60) id - 60 else id
            }.thenBy { level ->
                 // Ascended comes second
                 val id = level.levelId.filter { it.isDigit() }.toIntOrNull() ?: -1
                 id > 60
            })
            
            val mainCompleted = LevelProgressManager.getCompletedGalleryLevels(context)
            val ascendedCompleted = LevelProgressManager.getCompletedAscendedLevels(context)
            
            val virtualSet = mutableSetOf<Int>()
            virtualSet.addAll(mainCompleted)
            // Ascended IDs in Gallery start at 61 (corresponding to Main 1).
            // Ascended Level 0 (Tutorial) should NOT map to 60. Ignore it.
            ascendedCompleted.forEach { id -> 
                if (id > 0) virtualSet.add(60 + id) 
            }
            
            // Update State (Compose Snapshot system handles thread safety for these mutableStateOf writes, 
            // but doing it here ensures the heavy lifting is done in IO)
            memories = sortedMemories
            completedIds = virtualSet
        }
    }
    
    // Track scroll position for collapsible header
    val memoriesListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val blueprintListState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Header collapses when scrolled past threshold
    val isHeaderCollapsed by remember {
        derivedStateOf {
            when (selectedTab) {
                0 -> memoriesListState.firstVisibleItemIndex > 0 || memoriesListState.firstVisibleItemScrollOffset > 50
                1 -> blueprintListState.firstVisibleItemIndex > 0 || blueprintListState.firstVisibleItemScrollOffset > 50
                else -> false
            }
        }
    }

    GalaxyBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header stats - collapses when scrolling
            AscendedStatusHeader(completedIds, isCollapsed = isHeaderCollapsed)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SegmentedSwitch(
                    items = listOf(
                        stringResource(R.string.asc_tab_memories),
                        stringResource(R.string.asc_tab_blueprint)
                    ),
                    selectedIndex = selectedTab,
                    onSelectionChange = { selectedTab = it }
                )
            }

            when (selectedTab) {
                0 -> MemoriesView(memories, completedIds, onLocateLevel, listState = memoriesListState, preloadedNativeAd = preloadedNativeAd)
                1 -> BlueprintView(completedIds, listState = blueprintListState, preloadedNativeAd = preloadedNativeAd)
            }
        }
    }
}

@Composable
fun AscendedStatusHeader(completedIds: Set<Int>, isCollapsed: Boolean = false) {
    val completedCount = remember(completedIds) { completedIds.filter { it in 1..120 }.size }
    val completionRate = if (completedCount > 0) (completedCount * 100 / 120) else 0
    
    // Animate height and alpha based on collapsed state
    val collapsedProgress by animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = tween(250),
        label = "collapse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(
                top = androidx.compose.ui.unit.lerp(32.dp, 8.dp, collapsedProgress),
                bottom = androidx.compose.ui.unit.lerp(12.dp, 4.dp, collapsedProgress)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title - always visible but smaller when collapsed
        Text(
            text = stringResource(R.string.asc_project_title),
            fontSize = androidx.compose.ui.unit.lerp(12.sp, 10.sp, collapsedProgress),
            fontWeight = FontWeight.Black,
            color = Color(0xFF2979FF),
            letterSpacing = 2.sp
        )
        
        // Large percentage - shrinks when collapsed
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$completionRate%",
                fontSize = androidx.compose.ui.unit.lerp(48.sp, 24.sp, collapsedProgress),
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            
            // Show compact fragment count when collapsed
            if (isCollapsed) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "($completedCount/120)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        
        // Progress bar and status - hidden when collapsed
        AnimatedVisibility(
            visible = !isCollapsed,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(completionRate / 100f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF2979FF), Color(0xFF00E676))
                                )
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.asc_status_synthesizing),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (completionRate == 100) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f)
                    )
                    Text(
                        text = stringResource(R.string.asc_fragments_count, completedCount, 120),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedSwitch(items: List<String>, selectedIndex: Int, onSelectionChange: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .padding(4.dp)
    ) {
        Row {
            items.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF2979FF).copy(alpha = 0.3f) 
                            else Color.Transparent
                        )
                        .border(
                            if (isSelected) 1.dp else 0.dp,
                            if (isSelected) Color(0xFF2979FF).copy(alpha = 0.5f) else Color.Transparent,
                            CircleShape
                        )
                        .clickable { onSelectionChange(index) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun MemoriesView(
    memories: List<LevelConfig>, 
    completedIds: Set<Int>, 
    onLocateLevel: (Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    preloadedNativeAd: com.google.android.gms.ads.nativead.NativeAd? = null
) {
    var showDetailImage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(memories.size) { index ->
            val level = memories[index]
            // Extract numeric ID from levelId (e.g. "g_1_A" -> 1, "g_61_B" -> 61)
            val numericId = level.levelId.filter { it.isDigit() }.toIntOrNull() ?: -1
            val isCompleted = completedIds.contains(numericId)
            MemoryItem(
                level = level, 
                isCompleted = isCompleted, 
                onClick = { 
                    if (isCompleted) {
                        val assetPath = when (val source = level.imageSource) {
                            is ImageSource.Asset -> "file:///android_asset/${source.path}"
                            else -> null
                         }
                         showDetailImage = assetPath
                    }
                },
                onLocate = { onLocateLevel(numericId) }
            )

            // Every 10 rows, insert a native ad
            if ((index + 1) % 10 == 0 && index != memories.size - 1) {
                site.aiok.onepic.ui.components.NativeAdView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    preloadedNativeAd = preloadedNativeAd,
                    loadDelayMillis = if (preloadedNativeAd != null) 0L else 1500L
                )
            }
        }
    }
    
    // Image Detail Dialog
    showDetailImage?.let { assetPath ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDetailImage = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showDetailImage = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = assetPath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MemoryItem(level: LevelConfig, isCompleted: Boolean, onClick: () -> Unit, onLocate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp) // Minimum height, but can expand
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable(enabled = isCompleted, onClick = onClick) // Add Clickable
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    val assetPath = when (val source = level.imageSource) {
                        is ImageSource.Asset -> "file:///android_asset/${source.path}"
                        else -> null
                    }
                    AsyncImage(
                        model = assetPath, 
                        contentDescription = null, 
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Lock, 
                        null, 
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isCompleted) level.title else stringResource(R.string.asc_memory_title), 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCompleted) (level.storyText ?: "No data logs found.") else stringResource(R.string.asc_memory_desc), 
                    maxLines = 2, 
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 16.sp
                )
            }
            
            // Locate Button
            IconButton(onClick = onLocate) {
                Icon(
                    imageVector = Icons.Default.LocationOn, 
                    contentDescription = "Locate",
                    tint = if (isCompleted) Color(0xFF2979FF) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun BlueprintView(
    completedIds: Set<Int>,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    preloadedNativeAd: com.google.android.gms.ads.nativead.NativeAd? = null
) {
    var completedCount by remember { mutableStateOf(0) }
    var moduleProgress by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    
    // Ids for strings mod_0, mod_1...
    val moduleIds = (0..12).toList()

    LaunchedEffect(completedIds) {
        val validIds = completedIds.filter { it in 1..120 }
        completedCount = validIds.size
        
        val progressMap = mutableMapOf<Int, Int>()
        moduleIds.forEachIndexed { index, modId ->
            if (index < 12) {
                val mainRange = (index * 5 + 1)..(index * 5 + 5)
                val ascendedRange = (60 + index * 5 + 1)..(60 + index * 5 + 5)
                val count = completedIds.count { it in mainRange || it in ascendedRange }
                progressMap[modId] = count 
            } else {
                val allOnline = (0..11).all { i ->
                    val mainRange = (i * 5 + 1)..(i * 5 + 5)
                    val ascendedRange = (60 + i * 5 + 1)..(60 + i * 5 + 5)
                    completedIds.count { it in mainRange || it in ascendedRange } == 10
                }
                progressMap[modId] = if (allOnline) 10 else 0
            }
        }
        moduleProgress = progressMap
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp) // Manual spacing with separators
        ) {
            items(moduleIds.size) { index ->
                val modId = moduleIds[index]
                val progress = moduleProgress[modId] ?: 0
                val isOnline = progress == 10
                
                if (index == 12) {
                    // Final Ark: Downward Arrow Synthesis
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "▼", 
                            fontSize = 24.sp,
                            color = if (isOnline) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        ModuleRow(modId, progress, isOnline, index)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ModuleRow(modId, progress, isOnline, index)
                        
                        // Add '+' sign between rows (except after Launch Key)
                        if (index < 11) {
                            Text(
                                "✚", 
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                // Every 4 items, insert a native ad
                if ((index + 1) % 4 == 0 && index != moduleIds.size - 1) {
                    site.aiok.onepic.ui.components.NativeAdView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        preloadedNativeAd = preloadedNativeAd,
                        loadDelayMillis = if (preloadedNativeAd != null) 0L else 1500L
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleRow(modId: Int, progress: Int, isOnline: Boolean, moduleIndex: Int) {
    var showOverlay by remember { mutableStateOf(false) }
    
    // Dynamically get string resource ID for module name
    val resId = when(modId) {
        0 -> R.string.mod_0
        1 -> R.string.mod_1
        2 -> R.string.mod_2
        3 -> R.string.mod_3
        4 -> R.string.mod_4
        5 -> R.string.mod_5
        6 -> R.string.mod_6
        7 -> R.string.mod_7
        8 -> R.string.mod_8
        9 -> R.string.mod_9
        10 -> R.string.mod_10
        11 -> R.string.mod_11
        else -> R.string.mod_12
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                1.dp, 
                if (isOnline) Color(0xFF00E676).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f), 
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(resId), 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // We no longer display EN name separately if redundant, but for theme we can keep a tech label
                Text(
                    text = "MODULE_ID: 0X${indexToHex(modId)}", 
                    fontSize = 12.sp,
                    color = if (isOnline) Color(0xFF00E676) else Color(0xFF2979FF).copy(alpha = 0.8f)
                )
            }
            StatusBadge(isOnline)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModuleSmallCard(
                assetPath = LevelRepository.getBlueprintAsset(moduleIndex), 
                isActive = progress > 0, 
                onClick = { showOverlay = true }
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$progress/10", 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "➔", 
                    fontSize = 20.sp,
                    color = if (isOnline) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f)
                )
            }
            
            ModuleSmallCard(
                assetPath = LevelRepository.getRenderAsset(moduleIndex), 
                isActive = isOnline, 
                isRender = true, 
                onClick = { if (isOnline) showOverlay = true }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress / 10f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = if (isOnline) Color(0xFF00E676) else Color(0xFF2979FF),
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }

    if (showOverlay) {
        RenderDetailDialog(stringResource(resId), progress, isOnline, moduleIndex) { showOverlay = false }
    }
}

private fun indexToHex(index: Int): String {
    return Integer.toHexString(index).uppercase()
}

@Composable
fun ModuleSmallCard(assetPath: String, isActive: Boolean, isRender: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isActive) Color.Black.copy(alpha = 0.3f) 
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isActive && isRender) 1.5.dp else 1.dp,
                color = if (isActive && isRender) Color(0xFF00E676).copy(alpha = 0.5f) 
                        else if (isActive) Color(0xFF2979FF).copy(alpha = 0.3f) 
                        else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = isActive, onClick = onClick)
    ) {
        AsyncImage(
            model = "file:///android_asset/$assetPath", 
            contentDescription = null, 
            contentScale = ContentScale.Crop, 
            modifier = Modifier.fillMaxSize().alpha(if (isActive) 1f else 0.2f)
        )
        if (!isActive) {
            Icon(
                Icons.Default.Lock, 
                null, 
                tint = Color.White.copy(alpha = 0.2f), 
                modifier = Modifier.align(Alignment.Center).size(20.dp)
            )
        }
    }
}

@Composable
fun StatusBadge(isOnline: Boolean) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isOnline) Color(0xFF00E676).copy(alpha = 0.2f) 
                else Color.White.copy(alpha = 0.1f)
            )
            .border(
                0.5.dp,
                if (isOnline) Color(0xFF00E676).copy(alpha = 0.3f) else Color.Transparent,
                CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (isOnline) stringResource(R.string.mod_status_online) else stringResource(R.string.mod_status_constructing), 
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOnline) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun RenderDetailDialog(name: String, progress: Int, isOnline: Boolean, moduleIndex: Int, onDismiss: () -> Unit) {
    val assetPath = if (isOnline) LevelRepository.getRenderAsset(moduleIndex) else LevelRepository.getBlueprintAsset(moduleIndex)
    
    val classicCount = (progress.coerceAtMost(5))
    val ascendedCount = (progress - 5).coerceAtLeast(0)
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss, 
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, 
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() }, 
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                
                // Image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = "file:///android_asset/$assetPath", 
                        contentDescription = null, 
                        contentScale = ContentScale.Fit, 
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Info Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E1E2C))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = name, 
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isOnline) 
                                stringResource(R.string.mod_detail_online)
                            else 
                                stringResource(R.string.mod_detail_constructing, progress, classicCount, ascendedCount), 
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onDismiss, 
                            modifier = Modifier.fillMaxWidth().height(50.dp), 
                            shape = RoundedCornerShape(12.dp), 
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnline) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                contentColor = if (isOnline) Color(0xFF00E676) else Color.White
                            )
                        ) {
                            Text(stringResource(R.string.mod_close))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
