package com.angelina.daytask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angelina.daytask.ui.theme.*
import kotlinx.coroutines.delay

enum class LandscapeType {
    MOUNTAIN, FOREST, DESERT
}

enum class Language {
    ES, EN
}

data class UserSettings(
    val darkTheme: Boolean? = null, // null = system
    val landscape: LandscapeType = LandscapeType.MOUNTAIN,
    val language: Language = Language.ES
)

data class Note(
    val title: String,
    val content: String,
    val date: String
)

data class Task(
    val name: String,
    val emoji: String,
    var completed: Boolean = false,
    val xp: Int = 10
)

@Composable
fun AppBackground(isDark: Boolean, content: @Composable () -> Unit) {
    val bgColors = if (isDark) {
        listOf(BgGradientStartDark, BgGradientEndDark)
    } else {
        listOf(BgGradientStart, BgGradientEnd)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        content()
    }
}

@Composable
fun AdventureProgressMap(
    tasks: List<Task>,
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    val completedCount = tasks.count { it.completed }
    val totalCount = tasks.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val density = LocalDensity.current.density

    val isDark = settings.darkTheme ?: isSystemInDarkTheme()

    val mapPhrases = if (settings.language == Language.ES) listOf(
        "¡Tu aventura acaba de comenzar!",
        "¡Un paso más cerca de la meta!",
        "¡Sigue el camino!",
        "Cada meta te acerca a tu destino.",
        "¡Mira cuánto has avanzado!",
        "El próximo punto está más cerca.",
        "¡Continúa tu recorrido!",
        "Tu camino, tus metas, tu progreso.",
        "¡La meta está cada vez más cerca!",
        "¡Sigue avanzando hacia la cima! 🏔️"
    ) else listOf(
        "Your adventure has just begun!",
        "One step closer to the goal!",
        "Keep following the path!",
        "Every goal brings you closer.",
        "Look how far you've come!",
        "The next point is closer.",
        "Continue your journey!",
        "Your path, your goals, your progress.",
        "The goal is getting closer!",
        "Keep moving toward the top! 🏔️"
    )

    val currentMapPhrase = remember(completedCount) {
        when {
            completedCount == 0 -> mapPhrases[0]
            completedCount == totalCount -> mapPhrases.last()
            else -> mapPhrases[(completedCount % (mapPhrases.size - 2)) + 1]
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000),
        label = "progressAnimation"
    )

    val skyColors = when (settings.landscape) {
        LandscapeType.MOUNTAIN -> if (isDark) listOf(Color(0xFF1A237E), Color(0xFF283593)) else listOf(Color(0xFF87CEEB), Color(0xFFE0F7FA))
        LandscapeType.FOREST -> if (isDark) listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)) else listOf(Color(0xFF4FC3F7), Color(0xFFE1F5FE))
        LandscapeType.DESERT -> if (isDark) listOf(Color(0xFF3E2723), Color(0xFF4E342E)) else listOf(Color(0xFFFFB74D), Color(0xFFFFF3E0))
    }

    val elementColor = when (settings.landscape) {
        LandscapeType.MOUNTAIN -> if (isDark) Color(0xFF455A64) else Color(0xFF90A4AE)
        LandscapeType.FOREST -> if (isDark) Color(0xFF004D40) else Color(0xFF2E7D32)
        LandscapeType.DESERT -> if (isDark) Color(0xFF5D4037) else Color(0xFFD4A373)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            val adventurePath = remember(width, height) {
                Path().apply {
                    moveTo(width * 0.1f, height * 0.85f)
                    cubicTo(
                        width * 0.3f, height * 0.7f,
                        width * 0.5f, height * 0.95f,
                        width * 0.9f, height * 0.75f
                    )
                }
            }

            val pathMeasure = remember(adventurePath) {
                PathMeasure().apply { setPath(adventurePath, false) }
            }

            val pathLength = pathMeasure.length
            val indicatorPos = pathMeasure.getPosition(pathLength * animatedProgress)

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = Brush.verticalGradient(colors = skyColors))

                if (isDark) {
                    val starPositions = listOf(0.1f to 0.2f, 0.3f to 0.15f, 0.5f to 0.25f, 0.7f to 0.1f, 0.85f to 0.2f, 0.95f to 0.3f)
                    starPositions.forEach { (sx, sy) ->
                        drawCircle(Color.White.copy(alpha = 0.8f), radius = 1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * sx, height * sy))
                    }
                }

                when (settings.landscape) {
                    LandscapeType.MOUNTAIN -> {
                        drawPath(Path().apply {
                            moveTo(width * 0.1f, height * 0.85f)
                            lineTo(width * 0.35f, height * 0.5f)
                            lineTo(width * 0.6f, height * 0.85f)
                            close()
                        }, elementColor.copy(alpha = 0.5f))
                        
                        val mPeakX = width * 0.6f
                        val mPeakY = height * 0.35f
                        drawPath(Path().apply {
                            moveTo(width * 0.3f, height * 0.9f)
                            lineTo(mPeakX, mPeakY)
                            lineTo(mPeakX, height * 0.9f)
                            close()
                        }, elementColor.copy(alpha = 0.9f))
                        drawPath(Path().apply {
                            moveTo(mPeakX, height * 0.9f)
                            lineTo(mPeakX, mPeakY)
                            lineTo(width * 0.9f, height * 0.9f)
                            close()
                        }, elementColor)
                        drawPath(Path().apply {
                            moveTo(mPeakX, mPeakY)
                            lineTo(mPeakX - width * 0.05f, mPeakY + height * 0.1f)
                            lineTo(mPeakX, mPeakY + height * 0.07f)
                            lineTo(mPeakX + width * 0.05f, mPeakY + height * 0.1f)
                            close()
                        }, Color.White.copy(alpha = 0.9f))
                    }
                    LandscapeType.FOREST -> {
                        for (i in 0..6) {
                            val tx = width * (0.05f + i * 0.15f)
                            val th = height * (0.55f + (i % 3) * 0.05f)
                            drawRect(Color(0xFF5D4037), topLeft = androidx.compose.ui.geometry.Offset(tx - 4.dp.toPx(), th), size = androidx.compose.ui.geometry.Size(8.dp.toPx(), height * 0.3f))
                            drawCircle(elementColor.copy(alpha = 0.8f), radius = 25.dp.toPx(), center = androidx.compose.ui.geometry.Offset(tx, th))
                            drawCircle(elementColor, radius = 20.dp.toPx(), center = androidx.compose.ui.geometry.Offset(tx, th - 10.dp.toPx()))
                            drawCircle(elementColor.copy(alpha = 0.9f), radius = 15.dp.toPx(), center = androidx.compose.ui.geometry.Offset(tx, th - 20.dp.toPx()))
                        }
                    }
                    LandscapeType.DESERT -> {
                        drawPath(Path().apply {
                            moveTo(0f, height * 0.8f)
                            quadraticTo(width * 0.3f, height * 0.55f, width * 0.6f, height * 0.8f)
                            quadraticTo(width * 0.8f, height * 0.95f, width, height * 0.75f)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }, elementColor.copy(alpha = 0.8f))
                        drawPath(Path().apply {
                            moveTo(width * 0.4f, height * 0.9f)
                            quadraticTo(width * 0.75f, height * 0.65f, width, height * 0.9f)
                            lineTo(width, height)
                            lineTo(width * 0.4f, height)
                            close()
                        }, elementColor)
                    }
                }

                if (settings.landscape != LandscapeType.DESERT) {
                    val cloudColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.7f)
                    listOf(width * 0.2f to height * 0.25f, width * 0.75f to height * 0.2f).forEach { (cx, cy) ->
                        drawCircle(cloudColor, radius = 25.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(cloudColor, radius = 20.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx - 15.dp.toPx(), cy + 5.dp.toPx()))
                        drawCircle(cloudColor, radius = 20.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx + 15.dp.toPx(), cy + 5.dp.toPx()))
                    }
                } else {
                    val bodyColor = if (isDark) Color(0xFFE1F5FE) else Color(0xFFFFD54F)
                    drawCircle(bodyColor.copy(alpha = 0.2f), radius = 50.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.2f))
                    drawCircle(bodyColor, radius = 35.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.2f))
                }

                drawPath(
                    path = adventurePath,
                    color = if (settings.landscape == LandscapeType.DESERT) Color(0xFFBC8F8F) else Color(0xFF8D6E63),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                for (i in 0..totalCount) {
                    val fraction = if (totalCount > 0) i.toFloat() / totalCount else 0f
                    val milestonePos = pathMeasure.getPosition(pathLength * fraction)
                    val isReached = i <= completedCount
                    val color = if (isReached) GoldReward else Color(0xFFBDBDBD)
                    drawCircle(color = color, radius = (if (isReached) 8.dp else 6.dp).toPx(), center = milestonePos)
                    if (isReached && i > 0) {
                        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 12.dp.toPx(), center = milestonePos, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            val endPos = pathMeasure.getPosition(pathLength)
            Text(
                text = "🚩",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = (endPos.x / density).dp - 12.dp, top = (endPos.y / density).dp - 35.dp),
                fontSize = 24.sp
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = (indicatorPos.x / density).dp - 15.dp, top = (indicatorPos.y / density).dp - 35.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .background(Color(0xFF6C4CF1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏃", modifier = Modifier.graphicsLayer(scaleX = -1f), fontSize = 16.sp)
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = animatedProgress >= 0.99f && totalCount > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (settings.language == Language.ES) "¡Increíble! 🌟" else "Amazing! 🌟",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (settings.language == Language.ES) "Has llegado a la meta" else "You've reached the goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "✨ ✨ ✨", fontSize = 20.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (settings.language == Language.ES) "Tu progreso de aventura" else "Your adventure progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$completedCount de $totalCount metas completadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentMapPhrase,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DayTaskApp()
        }
    }
}

@Composable
fun DayTaskApp() {
    var screen by remember { mutableStateOf("splash") }
    var settings by remember { mutableStateOf(UserSettings()) }
    var currentTab by remember { mutableStateOf("home") }

    val tasks = remember {
        mutableStateListOf(
            Task("Tomar desayuno", "🍳"),
            Task("Estudiar", "📚"),
            Task("Hacer ejercicio", "🏃"),
            Task("Leer", "📖")
        )
    }

    val notes = remember { mutableStateListOf<Note>() }

    val isDark = settings.darkTheme ?: isSystemInDarkTheme()

    DayTaskTheme(darkTheme = isDark) {
        LaunchedEffect(Unit) {
            delay(1800)
            screen = "login"
        }

        Scaffold(
            bottomBar = {
                if (screen == "home" || screen == "notes") {
                    NavigationBar(
                        containerColor = if (isDark) SurfaceDark else Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        NavigationBarItem(
                            selected = currentTab == "home",
                            onClick = { 
                                currentTab = "home"
                                screen = "home"
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(if (settings.language == Language.ES) "Hoy" else "Today") }
                        )
                        NavigationBarItem(
                            selected = currentTab == "notes",
                            onClick = { 
                                currentTab = "notes"
                                screen = "notes"
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            label = { Text(if (settings.language == Language.ES) "Notas" else "Notes") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (screen) {
                    "splash" -> AppBackground(isDark = isDark) { SplashScreen(isDark = isDark) }
                    "login" -> AppBackground(isDark = isDark) { LoginScreen(onLogin = { screen = "home" }) }
                    "home" -> AppBackground(isDark = isDark) {
                        HomeScreen(tasks = tasks, settings = settings, onNavigateToSettings = { screen = "settings" })
                    }
                    "notes" -> AppBackground(isDark = isDark) {
                        NotesScreen(notes = notes, settings = settings, onNavigateToSettings = { screen = "settings" })
                    }
                    "settings" -> AppBackground(isDark = isDark) {
                        SettingsScreen(settings = settings, onSettingsChange = { settings = it }, onBack = { screen = currentTab })
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, isDark: Boolean, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onBack: () -> Unit
) {
    val isDark = settings.darkTheme ?: isSystemInDarkTheme()
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = if (settings.language == Language.ES) "Ajustes" else "Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = if (settings.language == Language.ES) "Tema" else "Theme", isDark = isDark) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ThemeOption(if (settings.language == Language.ES) "Claro" else "Light", settings.darkTheme == false) { onSettingsChange(settings.copy(darkTheme = false)) }
                    ThemeOption(if (settings.language == Language.ES) "Oscuro" else "Dark", settings.darkTheme == true) { onSettingsChange(settings.copy(darkTheme = true)) }
                    ThemeOption(if (settings.language == Language.ES) "Sistema" else "System", settings.darkTheme == null) { onSettingsChange(settings.copy(darkTheme = null)) }
                }
            }
            SettingsSection(title = if (settings.language == Language.ES) "Paisaje" else "Landscape", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LandscapeType.entries.forEach { type ->
                        LandscapeOption(type.name, settings.landscape == type) { onSettingsChange(settings.copy(landscape = type)) }
                    }
                }
            }
            SettingsSection(title = if (settings.language == Language.ES) "Idioma" else "Language", isDark = isDark) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LanguageOption("Español", settings.language == Language.ES) { onSettingsChange(settings.copy(language = Language.ES)) }
                    LanguageOption("English", settings.language == Language.EN) { onSettingsChange(settings.copy(language = Language.EN)) }
                }
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryPurple,
            selectedLabelColor = Color.White,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun LandscapeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ThemeOption(label, selected, onClick)
}

@Composable
fun SplashScreen(isDark: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(110.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryPurpleDark))).padding(2.dp).background(if (isDark) SurfaceDark else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "☀️", style = MaterialTheme.typography.displayMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "DayTask", color = if (isDark) PrimaryPurpleDark else PrimaryPurple, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = if (isDark) "Organize your day. Reach your goals." else "Organiza tu día. Alcanza tus metas.", color = TextGray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(35.dp))
            CircularProgressIndicator(modifier = Modifier.size(35.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(65.dp))
        Box(modifier = Modifier.size(85.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))), contentAlignment = Alignment.Center) {
            Text(text = "☀️", style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Bienvenido a DayTask", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Organiza tus actividades y haz que cada día cuente.", textAlign = TextAlign.Center, color = TextGray)
        Spacer(modifier = Modifier.height(35.dp))
        OutlinedTextField(value = email, onValueChange = { email = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, placeholder = { Text("ejemplo@correo.com") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(14.dp))
        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(25.dp))
        Button(onClick = { if (email.isBlank() || password.isBlank()) error = "Completa el correo y la contraseña." else onLogin() }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text(text = "Iniciar sesión", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = {}) { Text(text = "¿No tienes una cuenta? Crear cuenta", color = MaterialTheme.colorScheme.primary) }
        Spacer(modifier = Modifier.height(25.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(text = "  o  ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Continuar con Google") }
    }
}

@Composable
fun HomeScreen(tasks: SnapshotStateList<Task>, settings: UserSettings, onNavigateToSettings: () -> Unit) {
    val generalPhrases = if (settings.language == Language.ES) listOf("Cada paso cuenta.", "Sigue avanzando.", "Tú puedes lograrlo.", "Un día a la vez.", "No te detengas ahora.") else listOf("Every step counts.", "Keep moving forward.", "You can do it.", "One day at a time.", "Don't stop now.")
    val completionPhrases = if (settings.language == Language.ES) listOf("¡Meta completada! 🎉", "¡Excelente trabajo!", "¡Lo lograste!", "¡Un paso más!") else listOf("Goal completed! 🎉", "Excellent work!", "You did it!", "One more step!")
    val randomPhrase = remember { generalPhrases.random() }
    var lastCompletionPhrase by remember { mutableStateOf("") }
    var showCompletionMessage by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showCompletionMessage) { if (showCompletionMessage) { delay(2500); showCompletionMessage = false } }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding() + 100.dp)) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = if (settings.language == Language.ES) "☀️ Buenos días" else "☀️ Good morning", style = MaterialTheme.typography.bodyLarge, color = TextGray)
                            Text(text = if (settings.language == Language.ES) "Mi día" else "My day", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = randomPhrase, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    AdventureProgressMap(tasks = tasks, settings = settings)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = if (settings.language == Language.ES) "Actividades" else "Activities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                items(tasks) { task ->
                    TaskCard(task) { checked ->
                        val index = tasks.indexOf(task)
                        if (index != -1) {
                            tasks[index] = task.copy(completed = checked)
                            if (checked) { lastCompletionPhrase = completionPhrases.random(); showCompletionMessage = true }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = showCompletionMessage, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                    Text(text = lastCompletionPhrase, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showAddTaskDialog) {
        var newTaskName by remember { mutableStateOf("") }
        var newTaskEmoji by remember { mutableStateOf("🎯") }
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text(text = if (settings.language == Language.ES) "Nueva Actividad" else "New Activity", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newTaskName, onValueChange = { newTaskName = it }, label = { Text(if (settings.language == Language.ES) "Nombre" else "Name") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (settings.language == Language.ES) "Icono:" else "Icon:")
                        listOf("🎯", "📚", "🍳", "🏃", "💡", "🧘").forEach { emoji ->
                            Box(modifier = Modifier.size(35.dp).clip(CircleShape).background(if (newTaskEmoji == emoji) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent).clickable { newTaskEmoji = emoji }.padding(4.dp), contentAlignment = Alignment.Center) { Text(text = emoji) }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { if (newTaskName.isNotBlank()) { tasks.add(Task(newTaskName, newTaskEmoji)); showAddTaskDialog = false } }) { Text("Guardar") } }
        )
    }
}

@Composable
fun TaskCard(task: Task, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(IconContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = task.emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (task.completed) "Completada ✓" else "Pendiente",
                    color = if (task.completed) SuccessGreen else TextGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(checked = task.completed, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun NotesScreen(notes: SnapshotStateList<Note>, settings: UserSettings, onNavigateToSettings: () -> Unit) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNoteDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 20.dp, bottom = paddingValues.calculateBottomPadding() + 100.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = if (settings.language == Language.ES) "Mis Notas" else "My Notes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            text = if (settings.language == Language.ES) "Guarda tus pensamientos." else "Save your thoughts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (notes.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📝", fontSize = 60.sp)
                        Text(text = if (settings.language == Language.ES) "Aún no tienes notas" else "No notes yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    }
                }
            } else {
                items(notes) { note ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = note.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text(text = if (settings.language == Language.ES) "Nueva Nota" else "New Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = noteTitle, onValueChange = { noteTitle = it }, label = { Text("Título") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = noteContent, onValueChange = { noteContent = it }, label = { Text("Contenido") }, modifier = Modifier.height(150.dp), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = { if (noteTitle.isNotBlank()) { val sdf = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault()); notes.add(Note(noteTitle, noteContent, sdf.format(java.util.Date()))); showAddNoteDialog = false } }) { Text("Guardar") } }
        )
    }
}
