package com.example.appletvlauncher

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// -------------------- МОДИФИКАТОРЫ --------------------

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvClickable(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    this.focusable().combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvClickable(onClick: () -> Unit): Modifier = composed {
    this.focusable().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
}

// -------------------- КНОПКИ --------------------

@Composable
fun ActionCircleButton(icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    Box(
        modifier = modifier.size(56.dp).scale(scale).clip(RoundedCornerShape(28.dp))
            .background(if (isFocused) Color(0xFF34C759) else Color(0x33FFFFFF))
            .onFocusChanged { isFocused = it.isFocused }.tvClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 24.sp) }
}

@Composable
fun SettingsButton(modifier: Modifier = Modifier, color: Color = Color(0xFF6E8BFF), onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    Box(
        modifier = modifier.size(56.dp).scale(scale).clip(RoundedCornerShape(28.dp))
            .background(if (isFocused) color else Color(0x33FFFFFF)).onFocusChanged { isFocused = it.isFocused }.tvClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text("⚙", color = Color.White, fontSize = 24.sp) }
}

@Composable
fun AppleAddButton(modifier: Modifier = Modifier, width: Dp = 135.dp, height: Dp = 80.dp, corner: Dp = 16.dp, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    Box(
        modifier = modifier.width(width).height(height).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isFocused) 20.dp else 0.dp, RoundedCornerShape(corner)).clip(RoundedCornerShape(corner))
            .background(if (isFocused) Color(0xFFE0E0E0) else Color(0xFF2C2C2E))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(corner))
            .onFocusChanged { isFocused = it.isFocused }.tvClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text("+", color = if (isFocused) Color.Black else Color.White, fontSize = (height.value * 0.45f).sp, fontWeight = FontWeight.Light) }
}

// -------------------- КАРТОЧКИ --------------------

@Composable
fun DockItemCard(
    item: DockItem, index: Int, isFirst: Boolean, isLast: Boolean, settings: LauncherSettings, 
    iconCache: MutableMap<String, ImageBitmap>, coverCache: MutableMap<String, ImageBitmap>, dominantColorCache: MutableMap<String, Color>, appCovers: Map<String, String>,
    cardWidth: Dp, cardHeight: Dp, corner: Dp, focusScale: Float, isEditMode: Boolean, isGrabbed: Boolean, isAnyDialogOpen: Boolean, accentColor: Color, 
    focusRequester: FocusRequester, settingsButtonFocus: FocusRequester, editButtonFocus: FocusRequester, modifier: Modifier = Modifier, 
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 60L); appeared = true }

    LaunchedEffect(isGrabbed, index) { if (isGrabbed) runCatching { focusRequester.requestFocus() } }

    val appearScale by animateFloatAsState(if (appeared) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "appear")
    val currentFocusScale by animateFloatAsState(if (isFocused) focusScale else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "focusScale")
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isGrabbed) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    val totalScale = appearScale * currentFocusScale * (if (isGrabbed) pulseScale else 1f)
    val backgroundColor = if (isFocused) Color(0xFFE0E0E0) else Color(0xFF1C1C1E)
    val textColor = if (isFocused) Color.Black else Color.White
    val borderColor = if (isGrabbed) accentColor else if (isFocused) Color.White else Color(0x33FFFFFF)
    val borderWidth = if (isGrabbed || isFocused) 3.dp else 1.dp

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = !isAnyDialogOpen
                if (isFirst && !isEditMode) left = settingsButtonFocus
                if (isLast && !isEditMode) right = editButtonFocus
            }
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer { scaleX = totalScale; scaleY = totalScale }
            .shadow(if (isFocused || isGrabbed) 20.dp else 0.dp, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(corner))
            .onFocusChanged { state -> isFocused = state.isFocused }
            .tvClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.CenterStart
    ) {
        if (item.isFolder) {
            val firstApp = item.folderApps.firstOrNull()
            val imageBitmap = firstApp?.let { iconCache[it.packageName] }
            LaunchedEffect(firstApp) { 
                if (imageBitmap == null && firstApp != null) {
                    val bmp = withContext(Dispatchers.IO) { firstApp.icon.toBitmap().asImageBitmap() }
                    if (iconCache.size >= 100) iconCache.keys.firstOrNull()?.let { iconCache.remove(it) }
                    iconCache[firstApp.packageName] = bmp
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.offset(x = 4.dp, y = (-4).dp).size(cardHeight * 0.5f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.5f)))
                    Box(Modifier.offset(x = 2.dp, y = (-2).dp).size(cardHeight * 0.5f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.8f)))
                    Box(modifier = Modifier.size(cardHeight * 0.5f).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        if (imageBitmap != null) Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(cardHeight * 0.35f))
                        else Text("📁", fontSize = 20.sp)
                    }
                }
                if (settings.showLabels) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item.folderName, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            val app = item.app!!
            val imageBitmap = iconCache[app.packageName]
            LaunchedEffect(app.packageName) { 
                if (imageBitmap == null) {
                    val bmp = withContext(Dispatchers.IO) { app.icon.toBitmap().asImageBitmap() }
                    if (iconCache.size >= 100) iconCache.keys.firstOrNull()?.let { iconCache.remove(it) }
                    iconCache[app.packageName] = bmp
                }
            }
            val coverUri = appCovers[app.packageName]
            val coverBitmap = coverCache[app.packageName]

            if (coverUri != null && coverBitmap != null) {
                Image(bitmap = coverBitmap, contentDescription = app.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(cardHeight * 0.5f).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        if (imageBitmap != null) Image(bitmap = imageBitmap, contentDescription = app.name, modifier = Modifier.size(cardHeight * 0.35f))
                        else Text(app.name.take(1).uppercase(), color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    if (settings.showLabels) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(app.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun GridAppCard(
    app: AppInfo, index: Int, settings: LauncherSettings, 
    iconCache: MutableMap<String, ImageBitmap>, coverCache: MutableMap<String, ImageBitmap>, appCovers: Map<String, String>,
    cardWidth: Dp, cardHeight: Dp, corner: Dp, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(if (isFocused) 1.08f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "focusScale")

    val imageBitmap = iconCache[app.packageName]
    LaunchedEffect(app.packageName) { 
        if (imageBitmap == null) {
            val bmp = withContext(Dispatchers.IO) { app.icon.toBitmap().asImageBitmap() }
            if (iconCache.size >= 100) iconCache.keys.firstOrNull()?.let { iconCache.remove(it) }
            iconCache[app.packageName] = bmp
        }
    }

    val coverUri = appCovers[app.packageName]
    val coverBitmap = coverCache[app.packageName]

    Box(
        modifier = modifier.width(cardWidth).height(cardHeight).graphicsLayer { scaleX = focusScale; scaleY = focusScale }
            .shadow(if (isFocused) 20.dp else 0.dp, RoundedCornerShape(corner)).clip(RoundedCornerShape(corner)).background(Color(0xFF1C1C1E))
            .border(if (isFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(corner)).onFocusChanged { isFocused = it.isFocused }.tvClickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        if (coverUri != null && coverBitmap != null) {
            Image(bitmap = coverBitmap, contentDescription = app.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(cardHeight * 0.55f).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    if (imageBitmap != null) Image(bitmap = imageBitmap, contentDescription = app.name, modifier = Modifier.size(cardHeight * 0.40f))
                    else Text(app.name.take(1).uppercase(), color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                if (settings.showLabels) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(app.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun RecentAppCard(
    app: AppInfo, index: Int, total: Int, iconCache: MutableMap<String, ImageBitmap>, accentColor: Color, isAnyDialogOpen: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    
    val imageBitmap = iconCache[app.packageName]
    LaunchedEffect(app.packageName) {
        if (imageBitmap == null) {
            val bmp = withContext(Dispatchers.IO) { app.icon.toBitmap().asImageBitmap() }
            if (iconCache.size >= 100) iconCache.keys.firstOrNull()?.let { iconCache.remove(it) }
            iconCache[app.packageName] = bmp
        }
    }

    Box(
        modifier = modifier.size(64.dp).scale(scale).clip(RoundedCornerShape(16.dp)).background(if (isFocused) Color.White else Color(0x55FFFFFF))
            .border(if (isFocused) 2.dp else 0.dp, accentColor, RoundedCornerShape(16.dp)).focusProperties { canFocus = !isAnyDialogOpen }
            .onFocusChanged { isFocused = it.isFocused }.tvClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) Image(bitmap = imageBitmap, contentDescription = app.name, modifier = Modifier.size(40.dp))
        else Text(app.name.take(1).uppercase(), color = if (isFocused) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

// -------------------- ДИАЛОГИ И МЕНЮ --------------------

@Composable
fun LauncherPinDialog(
    title: String, subtitle: String, pinValue: String, onPinChange: (String) -> Unit, errorText: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
    focusRequester: FocusRequester, pinField2Focus: FocusRequester? = null, cancelFocus: FocusRequester, confirmFocus: FocusRequester,
    isChangePin: Boolean = false, newPinValue: String = "", onNewPinChange: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { onDismiss() }, contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp)).padding(32.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .focusGroup().onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) true else false }, 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.Gray, fontSize = 16.sp)
            Text(subtitle, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(16.dp))
            
            BasicTextField(
                value = pinValue, onValueChange = onPinChange,
                textStyle = TextStyle(color = Color.White, fontSize = 32.sp, textAlign = TextAlign.Center), cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp)).padding(16.dp)
                    .focusRequester(focusRequester).focusProperties { if (isChangePin && pinField2Focus != null) down = pinField2Focus else down = cancelFocus },
                decorationBox = { innerTextField -> 
                    if (pinValue.isEmpty()) Text(if (isChangePin) "Текущий PIN" else "PIN", color = Color.Gray, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    innerTextField() 
                }
            )
            
            if (isChangePin) {
                Spacer(modifier = Modifier.height(12.dp))
                BasicTextField(
                    value = newPinValue, onValueChange = onNewPinChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 32.sp, textAlign = TextAlign.Center), cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp)).padding(16.dp)
                        .then(if (pinField2Focus != null) Modifier.focusRequester(pinField2Focus) else Modifier).focusProperties { down = cancelFocus },
                    decorationBox = { innerTextField -> 
                        if (newPinValue.isEmpty()) Text("Новый PIN", color = Color.Gray, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        innerTextField() 
                    }
                )
            }
            
            if (errorText.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(errorText, color = Color.Red, fontSize = 14.sp) }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Отмена", color = Color.Gray, fontSize = 18.sp, modifier = Modifier
                    .focusRequester(cancelFocus).focusProperties { up = if (isChangePin && pinField2Focus != null) pinField2Focus else focusRequester; right = confirmFocus }
                    .tvClickable(onClick = onDismiss)
                )
                Text("Сохранить", color = Color(0xFF6E8BFF), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier
                    .focusRequester(confirmFocus).focusProperties { up = if (isChangePin && pinField2Focus != null) pinField2Focus else focusRequester; left = cancelFocus }
                    .tvClickable(onClick = onConfirm)
                )
            }
        }
    }
}

@Composable
fun ContextMenuItem(text: String, modifier: Modifier = Modifier, focusRequester: FocusRequester, isDestructive: Boolean = false, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused && isDestructive) Color(0xFFFF453A) else if (isFocused) Color.White else Color.Transparent, label = "bgColor")
    val textColor by animateColorAsState(if (isFocused && isDestructive) Color.White else if (isFocused) Color.Black else Color.White.copy(alpha = 0.8f), label = "textColor")
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "scale")

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).scale(scale).background(bgColor, RoundedCornerShape(16.dp))
            .focusRequester(focusRequester).onFocusChanged { isFocused = it.isFocused }.tvClickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = textColor, fontSize = 18.sp, fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium)
    }
}

// -------------------- НАСТРОЙКИ --------------------

@Composable
fun SettingsSection(title: String, first: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    if (!first) Spacer(modifier = Modifier.height(32.dp))
    Text(title, color = Color(0xFF6E8BFF), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun SettingRow(label: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) Color(0xEEFFFFFF) else Color(0x15FFFFFF), label = "bg")
    val textColor by animateColorAsState(if (isFocused) Color.Black else Color.White, label = "text")
    Row(
        modifier = modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(16.dp)).onFocusChanged { isFocused = it.isFocused }
            .tvClickable { onCheckedChange(!checked) }.padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textColor, fontSize = 18.sp, fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal)
        Text(if (checked) "ВКЛ" else "ВЫКЛ", color = if (isFocused) Color.Black.copy(alpha = 0.6f) else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingCycle(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) Color(0xEEFFFFFF) else Color(0x15FFFFFF), label = "bg")
    val textColor by animateColorAsState(if (isFocused) Color.Black else Color.White, label = "text")
    Row(
        modifier = modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(16.dp)).onFocusChanged { isFocused = it.isFocused }
            .tvClickable { onChange(options[(options.indexOf(value) + 1) % options.size]) }.padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textColor, fontSize = 18.sp, fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal)
        Text("$value ▸", color = if (isFocused) Color.Black.copy(alpha = 0.6f) else Color(0xFF6E8BFF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) Color(0xEEFFFFFF) else Color(0x15FFFFFF), label = "bg")
    val textColor by animateColorAsState(if (isFocused) Color.Black else Color.White, label = "text")
    Row(
        modifier = modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(16.dp)).onFocusChanged { isFocused = it.isFocused }
            .tvClickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textColor, fontSize = 18.sp, fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal)
        Text("▸", color = if (isFocused) Color.Black.copy(alpha = 0.6f) else Color.Gray, fontSize = 20.sp)
    }
}

// -------------------- ФОНЫ --------------------

@Composable
fun BackgroundFor(theme: String) {
    when (theme) {
        "cosmos" -> AnimatedCosmosBackground()
        "gradient" -> AnimatedGradientBackground()
        "midnight" -> Box(Modifier.fillMaxSize().background(Color.Black))
        "retro" -> AnimatedRetroBackground()
        else -> Box(Modifier.fillMaxSize().background(Color.Black))
    }
}

@Composable
fun AnimatedCosmosBackground() {
    val t = rememberInfiniteTransition(label = "cosmos")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B0B2B), Color(0xFF1B0B3B), Color(0xFF2B1B5B), Color(0xFF0B0B2B)), start = Offset(0f, shift * 800f), end = Offset(2000f, 2000f + shift * 800f))))
}

@Composable
fun AnimatedGradientBackground() {
    val t = rememberInfiniteTransition(label = "grad")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)), start = Offset(0f, shift * 500f), end = Offset(1500f, 1500f + shift * 500f))))
}

@Composable
fun AnimatedRetroBackground() {
    val t = rememberInfiniteTransition(label = "retro")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2A0845), Color(0xFF6441A5), Color(0xFFFB00FF), Color(0xFF00F0FF)), start = Offset(0f, shift * 400f), end = Offset(1200f, 1200f + shift * 400f))))
}
