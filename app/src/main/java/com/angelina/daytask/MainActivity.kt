package com.angelina.daytask

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.angelina.daytask.data.AppDatabase
import com.angelina.daytask.data.model.*
import com.angelina.daytask.ui.theme.*
import com.angelina.daytask.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        val viewModelFactory = MainViewModel.Factory(application, db.taskDao(), db.noteDao(), db.userDao())
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        setContent {
            DayTaskApp(viewModel)
        }
    }
}

@Composable
fun DayTaskApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("splash") }
    var currentTab by remember { mutableStateOf("home") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }

    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val settings = viewModel.userSettings

    val isDark = settings.darkTheme ?: isSystemInDarkTheme()

    fun handleLogin() {
        if (settings.isBiometricEnabled) {
            authenticateWithBiometrics(
                activity = activity,
                onSuccess = { 
                    isAuthenticated = true
                    screen = "home" 
                },
                onError = { /* Allow manual entry or show error */ }
            )
        } else {
            isAuthenticated = true
            screen = "home"
        }
    }

    DayTaskTheme(darkTheme = isDark) {
        LaunchedEffect(Unit) {
            delay(1800)
            screen = "login"
        }

        Scaffold(
            bottomBar = {
                if ((screen == "home" || screen == "notes") && isAuthenticated) {
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
                    "login" -> AppBackground(isDark = isDark) { 
                        LoginScreen(
                            onLogin = { email, pass ->
                                scope.launch {
                                    val user = viewModel.loginUser(email, pass)
                                    if (user != null) {
                                        handleLogin()
                                    } else {
                                        loginError = if (settings.language == Language.ES) 
                                            "Correo o contraseña incorrectos" else "Invalid email or password"
                                    }
                                }
                            },
                            onNavigateToRegister = { screen = "register" },
                            loginError = loginError,
                            settings = settings
                        ) 
                    }
                    "register" -> AppBackground(isDark = isDark) {
                        RegisterScreen(
                            onRegister = { name, email, pass ->
                                scope.launch {
                                    val success = viewModel.registerUser(com.angelina.daytask.data.UserEntity(email, pass, name))
                                    if (success) screen = "login"
                                }
                            },
                            onBack = { screen = "login" },
                            settings = settings
                        )
                    }
                    "home" -> AppBackground(isDark = isDark) {
                        HomeScreen(
                            tasks = tasks,
                            settings = settings,
                            onNavigateToSettings = { screen = "settings" },
                            onUpdateTask = { viewModel.updateTask(it) },
                            onAddTask = { viewModel.addTask(it) }
                        )
                    }
                    "notes" -> AppBackground(isDark = isDark) {
                        NotesScreen(
                            notes = notes,
                            settings = settings,
                            onNavigateToSettings = { screen = "settings" },
                            onAddNote = { viewModel.addNote(it) }
                        )
                    }
                    "settings" -> AppBackground(isDark = isDark) {
                        SettingsScreen(
                            settings = settings,
                            onSettingsChange = { viewModel.updateSettings(it) },
                            onBack = { screen = currentTab }
                        )
                    }
                }
            }
        }
    }
}

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
            Text(text = "DayTask", color = if (isDark) Color.White else PrimaryPurple, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = if (isDark) "Organize your day. Reach your goals." else "Organiza tu día. Alcanza tus metas.", color = TextGray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(35.dp))
            CircularProgressIndicator(modifier = Modifier.size(35.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    loginError: String,
    settings: UserSettings
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$".toRegex()

    LaunchedEffect(loginError) {
        if (loginError.isNotEmpty()) error = loginError
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(65.dp))
        Box(modifier = Modifier.size(85.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))), contentAlignment = Alignment.Center) {
            Text(text = "☀️", style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = if (settings.language == Language.ES) "Bienvenido a DayTask" else "Welcome to DayTask", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Haz que cada día cuente.", textAlign = TextAlign.Center, color = TextGray)
        Spacer(modifier = Modifier.height(35.dp))
        OutlinedTextField(value = email, onValueChange = { email = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, placeholder = { Text("ejemplo@correo.com") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(14.dp))
        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(25.dp))
        Button(onClick = { 
            if (email.isBlank() || password.isBlank()) error = "Completa el correo y la contraseña."
            else if (!email.matches(emailRegex)) error = "Correo no válido."
            else if (password.length < 6) error = "Mínimo 6 caracteres."
            else onLogin(email, password) 
        }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text(text = if (settings.language == Language.ES) "Iniciar sesión" else "Log In", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text(if (settings.language == Language.ES) "¿No tienes cuenta? Regístrate" else "Don't have an account? Sign Up")
        }
    }
}

@Composable
fun RegisterScreen(
    onRegister: (String, String, String) -> Unit,
    onBack: () -> Unit,
    settings: UserSettings
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$".toRegex()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = if (settings.language == Language.ES) "Crear Cuenta" else "Create Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(35.dp))
        OutlinedTextField(value = name, onValueChange = { name = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre Completo") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(value = email, onValueChange = { email = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, placeholder = { Text("ejemplo@correo.com") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(14.dp))
        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(25.dp))
        Button(onClick = { 
            if (name.isBlank() || email.isBlank() || password.isBlank()) error = "Completa todos los campos."
            else if (!email.matches(emailRegex)) error = "Correo no válido."
            else if (password.length < 6) error = "La contraseña debe tener al menos 6 caracteres."
            else onRegister(name, email, password) 
        }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text(text = if (settings.language == Language.ES) "Registrarse" else "Sign Up", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HomeScreen(tasks: List<Task>, settings: UserSettings, onNavigateToSettings: () -> Unit, onUpdateTask: (Task) -> Unit, onAddTask: (Task) -> Unit) {
    val completionPhrases = if (settings.language == Language.ES) listOf("¡Meta completada! 🎉", "¡Excelente trabajo!", "¡Lo lograste!", "¡Un paso más!") else listOf("Goal completed! 🎉", "Excellent work!", "You did it!", "One more step!")
    var lastCompletionPhrase by remember { mutableStateOf("") }
    var showCompletionMessage by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showCompletionMessage) { if (showCompletionMessage) { delay(2500); showCompletionMessage = false } }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 20.dp, bottom = 100.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = if (settings.language == Language.ES) "☀️ Buenos días" else "☀️ Good morning", style = MaterialTheme.typography.bodyLarge, color = TextGray)
                            Text(text = if (settings.language == Language.ES) "Mi día" else "My day", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    AdventureProgressMap(tasks = tasks, settings = settings)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = if (settings.language == Language.ES) "Actividades" else "Activities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                items(tasks) { task ->
                    TaskCard(task) { checked ->
                        onUpdateTask(task.copy(completed = checked))
                        if (checked) { lastCompletionPhrase = completionPhrases.random(); showCompletionMessage = true }
                    }
                }
            }
            AnimatedVisibility(visible = showCompletionMessage, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SuccessGreen), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                    Text(text = lastCompletionPhrase, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showAddTaskDialog) {
        AddTaskDialog(settings = settings, onDismiss = { showAddTaskDialog = false }, onConfirm = { n, e, d, t -> onAddTask(Task(name = n, emoji = e, day = d, time = t)) })
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
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(IconContainerLight), contentAlignment = Alignment.Center) {
                Text(text = task.emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${task.day} · ${task.time}", style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
                Text(text = if (task.completed) "Completada ✓" else "Pendiente", color = if (task.completed) SuccessGreen else TextGray, style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(checked = task.completed, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun AddTaskDialog(settings: UserSettings, onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var n by remember { mutableStateOf("") }
    var e by remember { mutableStateOf("🎯") }
    var d by remember { mutableStateOf(java.text.SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())) }
    var t by remember { mutableStateOf(java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (settings.language == Language.ES) "Nueva Actividad" else "New Activity", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Nombre") }, shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = d, onValueChange = { d = it }, modifier = Modifier.weight(1f), label = { Text("Día") })
                    OutlinedTextField(value = t, onValueChange = { t = it }, modifier = Modifier.weight(0.7f), label = { Text("Hora") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🎯", "📚", "🍳", "🏃", "💡").forEach { emoji ->
                        Box(modifier = Modifier.size(35.dp).clip(CircleShape).background(if (e == emoji) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent).clickable { e = emoji }, contentAlignment = Alignment.Center) {
                            Text(text = emoji)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (n.isNotBlank()) { onConfirm(n, e, d, t); onDismiss() } }) { Text("Guardar") }
        }
    )
}

@Composable
fun NotesScreen(notes: List<Note>, settings: UserSettings, onNavigateToSettings: () -> Unit, onAddNote: (Note) -> Unit) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNoteDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 20.dp, bottom = 100.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = if (settings.language == Language.ES) "Mis Notas" else "My Notes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "Guarda tus pensamientos.", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                    }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, null) }
                }
            }
            items(notes) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(text = note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = note.date, style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
                        Spacer(Modifier.height(8.dp))
                        Text(text = note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text(text = "Nueva Nota", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = content, onValueChange = { content = it }, Modifier.height(150.dp), label = { Text("Contenido") }, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = { 
                    if (title.isNotBlank()) { 
                        onAddNote(Note(title = title, content = content, date = java.text.SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())))
                        showAddNoteDialog = false 
                    } 
                }) { Text("Guardar") }
            }
        )
    }
}

@Composable
fun AdventureProgressMap(tasks: List<Task>, settings: UserSettings, modifier: Modifier = Modifier) {
    val completedCount = tasks.count { it.completed }
    val totalCount = tasks.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val density = LocalDensity.current.density
    val isDark = settings.darkTheme ?: isSystemInDarkTheme()
    val animatedProgress by animateFloatAsState(progressFraction, tween(1000), label = "p")
    
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

    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val width = this.constraints.maxWidth.toFloat()
            val height = this.constraints.maxHeight.toFloat()
            val adventurePath = Path().apply { moveTo(width * 0.1f, height * 0.8f); cubicTo(width * 0.3f, height * 0.6f, width * 0.5f, height * 0.9f, width * 0.9f, height * 0.7f) }
            val pathMeasure = PathMeasure().apply { setPath(adventurePath, false) }
            val pathLength = pathMeasure.length
            val indicatorPos = pathMeasure.getPosition(pathLength * animatedProgress)
            
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(skyColors))
                
                if (isDark) {
                    val starPositions = listOf(0.1f to 0.2f, 0.3f to 0.15f, 0.5f to 0.25f, 0.7f to 0.1f, 0.85f to 0.2f, 0.95f to 0.3f)
                    starPositions.forEach { (sx, sy) ->
                        drawCircle(Color.White.copy(alpha = 0.8f), radius = 1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * sx, height * sy))
                    }
                }

                when (settings.landscape) {
                    LandscapeType.MOUNTAIN -> {
                        drawPath(Path().apply {
                            moveTo(width * 0.1f, height * 0.8f)
                            lineTo(width * 0.35f, height * 0.5f)
                            lineTo(width * 0.6f, height * 0.8f)
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

                drawPath(adventurePath, Color.White.copy(alpha = 0.5f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                for (i in 0..totalCount) {
                    val f = if (totalCount > 0) i.toFloat() / totalCount else 0f
                    drawCircle(if (i <= completedCount) GoldReward else Color.White.copy(alpha = 0.4f), radius = 5.dp.toPx(), center = pathMeasure.getPosition(pathLength * f))
                }
            }
            Box(Modifier.align(Alignment.TopStart).padding(start = (indicatorPos.x / density).dp - 15.dp, top = (indicatorPos.y / density).dp - 35.dp).size(30.dp).clip(CircleShape).background(Color.White).padding(2.dp).background(PrimaryPurple, CircleShape), Alignment.Center) {
                Text("🏃", Modifier.graphicsLayer(scaleX = -1f), fontSize = 14.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(if (settings.language == Language.ES) "Progreso de Aventura" else "Adventure Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val mapPhrases = if (settings.language == Language.ES) listOf("¡Tu aventura comienza!", "¡Sigue así!", "¡Meta alcanzada!") else listOf("Adventure starts!", "Keep it up!", "Goal reached!")
                val phrase = when { completedCount == 0 -> mapPhrases[0]; completedCount == totalCount -> mapPhrases.last(); else -> mapPhrases[1] }
                Text(phrase, style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
            }
            Text("${(progressFraction * 100).toInt()}%", color = PrimaryPurple, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: UserSettings, onSettingsChange: (UserSettings) -> Unit, onBack: () -> Unit) {
    val isDark = settings.darkTheme ?: isSystemInDarkTheme()
    Scaffold(containerColor = Color.Transparent, topBar = { CenterAlignedTopAppBar(title = { Text(if (settings.language == Language.ES) "Ajustes" else "Settings", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsSection(title = if (settings.language == Language.ES) "Tema" else "Theme", isDark = isDark) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    ThemeOption(if (settings.language == Language.ES) "Claro" else "Light", settings.darkTheme == false) { onSettingsChange(settings.copy(darkTheme = false)) }
                    ThemeOption(if (settings.language == Language.ES) "Oscuro" else "Dark", settings.darkTheme == true) { onSettingsChange(settings.copy(darkTheme = true)) }
                    ThemeOption(if (settings.language == Language.ES) "Sistema" else "System", settings.darkTheme == null) { onSettingsChange(settings.copy(darkTheme = null)) }
                }
            }
            SettingsSection(title = if (settings.language == Language.ES) "Paisaje" else "Landscape", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            SettingsSection(title = if (settings.language == Language.ES) "Seguridad" else "Security", isDark = isDark) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(if (settings.language == Language.ES) "Bloqueo Biométrico" else "Biometric Lock", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = settings.isBiometricEnabled, onCheckedChange = { onSettingsChange(settings.copy(isBiometricEnabled = it)) })
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, isDark: Boolean, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.7f))) { Column(Modifier.padding(16.dp)) { content() } }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, shape = RoundedCornerShape(12.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryPurple, selectedLabelColor = Color.White))
}

@Composable
fun LandscapeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ThemeOption(label, selected, onClick)
}

fun authenticateWithBiometrics(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError(errString.toString()) }
    })
    val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Acceso Seguro").setSubtitle("Usa tu huella").setNegativeButtonText("Cancelar").build()
    biometricPrompt.authenticate(promptInfo)
}
