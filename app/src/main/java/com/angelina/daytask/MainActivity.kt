package com.angelina.daytask

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("splash") }
    var currentTab by remember { mutableStateOf("home") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }

    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val settings = viewModel.userSettings

    val isDark = settings.darkTheme

    fun handleLogin() {
        isAuthenticated = true
        screen = "home"
    }

    DayTaskTheme(darkTheme = isDark) {
        LaunchedEffect(Unit) {
            val savedUser = viewModel.checkSavedSession()
            delay(1500)
            if (savedUser != null) {
                handleLogin()
            } else {
                screen = "login"
            }
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
                            onClick = { currentTab = "home"; screen = "home" },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text(if (settings.language == Language.ES) "Hoy" else "Today") }
                        )
                        NavigationBarItem(
                            selected = currentTab == "notes",
                            onClick = { currentTab = "notes"; screen = "notes" },
                            icon = { Icon(Icons.Default.DateRange, null) },
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
                                    if (user != null) handleLogin()
                                    else loginError = if (settings.language == Language.ES) "Correo o contraseña incorrectos" else "Invalid email or password"
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
                            user = viewModel.currentUser,
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
                            onAddNote = { viewModel.addNote(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onTogglePin = { viewModel.updateNote(it.copy(isPinned = !it.isPinned)) }
                        )
                    }
                    "settings" -> AppBackground(isDark = isDark) {
                        SettingsScreen(
                            user = viewModel.currentUser,
                            settings = settings,
                            onSettingsChange = { viewModel.updateSettings(it) },
                            onBack = { screen = currentTab },
                            onLogout = {
                                viewModel.logout()
                                isAuthenticated = false
                                screen = "login"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBackground(isDark: Boolean, content: @Composable () -> Unit) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    
    val bgColors = remember(currentHour, isDark) {
        if (isDark) {
            listOf(BgGradientStartDark, BgGradientEndDark)
        } else {
            when {
                currentHour in 5..8 -> listOf(LightAmanecerStart, LightAmanecerEnd)
                currentHour in 9..16 -> listOf(BgGradientStart, BgGradientEnd)
                currentHour in 17..19 -> listOf(LightAtardecerStart, LightAtardecerEnd)
                else -> listOf(LightNocheStart, LightNocheEnd)
            }
        }
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
            Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryPurpleDark))).padding(2.dp).background(if (isDark) SurfaceDark else Color.White, CircleShape), contentAlignment = Alignment.Center) { Text("☀️", style = MaterialTheme.typography.displayMedium) }
            Spacer(Modifier.height(24.dp))
            Text("DayTask", color = if (isDark) Color.White else PrimaryPurple, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(if (isDark) "Make every day count." else "Haz que cada día cuente.", color = TextGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(35.dp))
            CircularProgressIndicator(modifier = Modifier.size(35.dp), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onNavigateToRegister: () -> Unit, loginError: String, settings: UserSettings) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$".toRegex()
    
    LaunchedEffect(loginError) { if (loginError.isNotEmpty()) error = loginError }
    
    fun performLogin() {
        if (email.isBlank() || password.isBlank()) error = "Completa los campos"
        else if (!email.matches(emailRegex)) error = "Correo no válido"
        else onLogin(email, password)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(65.dp))
        Box(
            modifier = Modifier
                .size(85.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) { Text("☀️", style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(20.dp))
        Text(if (settings.language == Language.ES) "Bienvenido a DayTask" else "Welcome to DayTask", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Haz que cada día cuente.", color = TextGray)
        Spacer(Modifier.height(35.dp))
        
        OutlinedTextField(
            value = email, 
            onValueChange = { email = it; error = "" }, 
            modifier = Modifier.fillMaxWidth(), 
            label = { Text("Correo electrónico") }, 
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true
        )
        
        Spacer(Modifier.height(14.dp))
        
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it; error = "" }, 
            modifier = Modifier.fillMaxWidth(), 
            label = { Text("Contraseña") }, 
            visualTransformation = PasswordVisualTransformation(), 
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { 
                focusManager.clearFocus()
                performLogin()
            }),
            singleLine = true
        )
        
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(25.dp))
        Button(onClick = { focusManager.clearFocus(); performLogin() }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(15.dp)) { Text(if (settings.language == Language.ES) "Iniciar sesión" else "Log In", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToRegister) { Text(if (settings.language == Language.ES) "¿No tienes cuenta? Regístrate" else "Don't have an account? Sign Up") }
    }
}

@Composable
fun RegisterScreen(onRegister: (String, String, String) -> Unit, onBack: () -> Unit, settings: UserSettings) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$".toRegex()

    fun performRegister() {
        if (name.isBlank() || email.isBlank() || password.isBlank()) error = "Completa los campos"
        else if (!email.matches(emailRegex)) error = "Email inválido"
        else if (password.length < 6) error = "Mínimo 6 caracteres"
        else onRegister(name, email, password)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        Text(if (settings.language == Language.ES) "Crear Cuenta" else "Create Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(35.dp))
        
        OutlinedTextField(
            value = name, 
            onValueChange = { name = it; error = "" }, 
            modifier = Modifier.fillMaxWidth(), 
            label = { Text("Nombre Completo") }, 
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true
        )
        
        Spacer(Modifier.height(14.dp))
        
        OutlinedTextField(
            value = email, 
            onValueChange = { email = it; error = "" }, 
            modifier = Modifier.fillMaxWidth(), 
            label = { Text("Email") }, 
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true
        )
        
        Spacer(Modifier.height(14.dp))
        
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it; error = "" }, 
            modifier = Modifier.fillMaxWidth(), 
            label = { Text("Contraseña") }, 
            visualTransformation = PasswordVisualTransformation(), 
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { 
                focusManager.clearFocus()
                performRegister()
            }),
            singleLine = true
        )
        
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(25.dp))
        Button(onClick = { focusManager.clearFocus(); performRegister() }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(15.dp)) { Text(if (settings.language == Language.ES) "Registrarse" else "Sign Up", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun HomeScreen(
    tasks: List<Task>,
    user: com.angelina.daytask.data.UserEntity?,
    settings: UserSettings,
    onNavigateToSettings: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onAddTask: (Task) -> Unit
) {
    val completionPhrases = if (settings.language == Language.ES) listOf("¡Meta completada! 🎉", "¡Excelente trabajo!") else listOf("Goal completed! 🎉", "Excellent work!")
    var lastPhrase by remember { mutableStateOf("") }
    var showMsg by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour in 5..11 -> if (settings.language == Language.ES) "☀️ Buenos días" else "☀️ Good morning"
        currentHour in 12..18 -> if (settings.language == Language.ES) "🌤️ Buenas tardes" else "🌤️ Good afternoon"
        else -> if (settings.language == Language.ES) "🌙 Buenas noches" else "🌙 Good evening"
    }

    LaunchedEffect(showMsg) { if (showMsg) { delay(2500); showMsg = false } }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }, containerColor = PrimaryPurple, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { p ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = p.calculateTopPadding() + 20.dp, bottom = 100.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(greeting, style = MaterialTheme.typography.bodyLarge, color = TextGray)
                            Text(if (settings.language == Language.ES) "Mi día" else "My day", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onNavigateToSettings) { Icon(Icons.Default.Settings, null) }
                    }
                    
                    UserLevelDisplay(user = user)

                    Spacer(modifier = Modifier.height(18.dp))
                    AdventureProgressMap(tasks = tasks, settings = settings)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(if (settings.language == Language.ES) "Actividades" else "Activities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                items(tasks) { task ->
                    TaskCard(task) { checked ->
                        onUpdateTask(task.copy(completed = checked))
                        if (checked) { lastPhrase = completionPhrases.random(); showMsg = true }
                    }
                }
            }
            AnimatedVisibility(visible = showMsg, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SuccessGreen), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) { 
                    Text(lastPhrase, Modifier.padding(horizontal = 24.dp, vertical = 12.dp), Color.White, fontWeight = FontWeight.Bold) 
                }
            }
        }
    }
    if (showAddTaskDialog) AddTaskDialog(settings, { showAddTaskDialog = false }) { n, e, d, t -> onAddTask(Task(name = n, emoji = e, day = d, time = t)) }
}

@Composable
fun UserLevelDisplay(user: com.angelina.daytask.data.UserEntity?) {
    if (user == null) return
    val progress = user.xp.toFloat() / 100f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "xp")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nivel ${user.level}", 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                if (user.streakCount > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${user.streakCount}", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
            Text(
                text = "${user.xp} / 100 XP", 
                style = MaterialTheme.typography.labelSmall, 
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PrimaryPurple,
            trackColor = PrimaryPurple.copy(alpha = 0.1f)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(IconContainerLight), Alignment.Center) { Text(task.emoji, style = MaterialTheme.typography.headlineSmall) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${task.day} · ${task.time}", style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
                Text(if (task.completed) "Completada ✓" else "Pendiente", color = if (task.completed) SuccessGreen else TextGray, style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(task.completed, onCheckedChange)
        }
    }
}

@Composable
fun AddTaskDialog(settings: UserSettings, onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var n by remember { mutableStateOf("") }
    var e by remember { mutableStateOf("🎯") }
    var d by remember { mutableStateOf(java.text.SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())) }
    var t by remember { mutableStateOf(java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val sel = Calendar.getInstance()
            sel.set(year, month, dayOfMonth)
            d = java.text.SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(sel.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hour, min ->
            t = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (settings.language == Language.ES) "Nueva Actividad" else "New Activity", fontWeight = FontWeight.Bold) }, text = {
        val focusManager = LocalFocusManager.current
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = n, 
                onValueChange = { n = it }, 
                label = { Text("Nombre") }, 
                shape = RoundedCornerShape(12.dp), 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                    OutlinedTextField(
                        value = d, 
                        onValueChange = {}, 
                        label = { Text("Día") }, 
                        readOnly = true, 
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Box(Modifier.weight(0.7f).clickable { timePickerDialog.show() }) {
                    OutlinedTextField(
                        value = t, 
                        onValueChange = {}, 
                        label = { Text("Hora") }, 
                        readOnly = true, 
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("🎯", "📚", "🍳", "🏃", "💡").forEach { emoji ->
                    Box(Modifier.size(35.dp).clip(CircleShape).background(if (e == emoji) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent).clickable { e = emoji }, Alignment.Center) { Text(emoji) }
                }
            }
        }
    }, confirmButton = { Button(onClick = { if (n.isNotBlank()) { onConfirm(n, e, d, t); onDismiss() } }) { Text("Guardar") } })
}

@Composable
fun NotesScreen(notes: List<Note>, settings: UserSettings, onNavigateToSettings: () -> Unit, onAddNote: (Note) -> Unit, onDeleteNote: (Note) -> Unit, onTogglePin: (Note) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<NoteCategory?>(null) }
    
    val filteredNotes = notes.filter { 
        (searchQuery.isEmpty() || it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true)) &&
        (selectedFilter == null || it.category == selectedFilter)
    }

    Scaffold(containerColor = Color.Transparent, floatingActionButton = { FloatingActionButton({ showDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Add, null) } }) { p ->
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = p.calculateTopPadding() + 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Column {
                    Text(if (settings.language == Language.ES) "Mis Notas" else "My Notes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Guarda tus pensamientos.", color = TextGray)
                }
                IconButton(onNavigateToSettings) { Icon(Icons.Default.Settings, null) }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(searchQuery, { searchQuery = it }, Modifier.fillMaxWidth(), placeholder = { Text(if (settings.language == Language.ES) "Buscar notas..." else "Search notes...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item { FilterChip(selectedFilter == null, { selectedFilter = null }, { Text("Todas") }) }
                items(NoteCategory.entries) { cat -> FilterChip(selectedFilter == cat, { selectedFilter = cat }, { Text(cat.name) }) }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(filteredNotes) { note ->
                    NoteCard(note, onDelete = { onDeleteNote(note) }, onTogglePin = { onTogglePin(note) })
                }
            }
        }
    }
    if (showDialog) AddNoteDialog(settings, onDismiss = { showDialog = false }, onConfirm = { t, c, cat -> 
        onAddNote(Note(title = t, content = c, category = cat, date = java.text.SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())))
        showDialog = false
    })
}

@Composable
fun NoteCard(note: Note, onDelete: () -> Unit, onTogglePin: () -> Unit) {
    val catColor = when(note.category) {
        NoteCategory.URGENT -> Color(0xFFFF5252)
        NoteCategory.WORK -> Color(0xFF448AFF)
        NoteCategory.IDEAS -> Color(0xFFFFD740)
        NoteCategory.PERSONAL -> PrimaryPurple
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (note.isPinned) 2.dp else 1.dp, if (note.isPinned) PrimaryPurple else catColor.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(note.category.name, style = MaterialTheme.typography.labelSmall, color = catColor, fontWeight = FontWeight.Bold)
                    if (note.isPinned) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Star, null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                    }
                }
                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                        Icon(if (note.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (note.isPinned) PrimaryPurple else TextGray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(note.date, style = MaterialTheme.typography.labelSmall, color = TextGray)
            Spacer(Modifier.height(8.dp))
            Text(note.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddNoteDialog(settings: UserSettings, onDismiss: () -> Unit, onConfirm: (String, String, NoteCategory) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(NoteCategory.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit, 
                    contentDescription = null, 
                    tint = PrimaryPurple, 
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (settings.language == Language.ES) "Nueva Nota" else "New Note",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            val focusManager = LocalFocusManager.current
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (settings.language == Language.ES) "Título" else "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (settings.language == Language.ES) "Contenido" else "Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (settings.language == Language.ES) "Categoría" else "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NoteCategory.entries) { cat ->
                            val isSelected = category == cat
                            val catColor = when(cat) {
                                NoteCategory.URGENT -> Color(0xFFFF5252)
                                NoteCategory.WORK -> Color(0xFF448AFF)
                                NoteCategory.IDEAS -> Color(0xFFFFD740)
                                NoteCategory.PERSONAL -> PrimaryPurple
                            }
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat.name) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor,
                                    selectedLabelColor = Color.White,
                                    labelColor = catColor.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = catColor.copy(alpha = 0.5f),
                                    selectedBorderColor = Color.Transparent,
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, content, category) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text(
                    text = if (settings.language == Language.ES) "Guardar" else "Save", 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (settings.language == Language.ES) "Cancelar" else "Cancel")
            }
        }
    )
}

@Composable
fun AdventureProgressMap(tasks: List<Task>, settings: UserSettings, modifier: Modifier = Modifier) {
    val completedCount = tasks.count { it.completed }
    val totalCount = tasks.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val density = LocalDensity.current.density
    val isDark = settings.darkTheme
    
    val infiniteTransition = rememberInfiniteTransition(label = "mapAnimation")
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudAnimation"
    )

    val bounceValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceAnimation"
    )

    val animatedProgress by animateFloatAsState(progressFraction, tween(1200), label = "p")
    
    val skyColors = when (settings.landscape) {
        LandscapeType.MOUNTAIN -> if (isDark) listOf(Color(0xFF0F0C29), Color(0xFF302B63)) else listOf(Color(0xFF87CEEB), Color(0xFFE0F7FA))
        LandscapeType.FOREST -> if (isDark) listOf(Color(0xFF0D1F0D), Color(0xFF1B5E20)) else listOf(Color(0xFFB2EBF2), Color(0xFFE1F5FE))
        LandscapeType.DESERT -> if (isDark) listOf(Color(0xFF2C1608), Color(0xFF5D4037)) else listOf(Color(0xFFFFB74D), Color(0xFFFFF3E0))
        LandscapeType.VOLCANO -> if (isDark) listOf(Color(0xFF212121), Color(0xFFB71C1C)) else listOf(Color(0xFFFF9800), Color(0xFFF44336))
    }
    
    val elementColor = when (settings.landscape) {
        LandscapeType.MOUNTAIN -> if (isDark) Color(0xFF455A64) else Color(0xFF90A4AE)
        LandscapeType.FOREST -> if (isDark) Color(0xFF1B5E20) else Color(0xFF2E7D32)
        LandscapeType.DESERT -> if (isDark) Color(0xFF5D4037) else Color(0xFFD4A373)
        LandscapeType.VOLCANO -> Color(0xFF1B1B1B)
    }

    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val width = this.constraints.maxWidth.toFloat()
            val height = this.constraints.maxHeight.toFloat()
            val adventurePath = Path().apply { moveTo(width * 0.1f, height * 0.8f); cubicTo(width * 0.3f, height * 0.6f, width * 0.5f, height * 0.9f, width * 0.9f, height * 0.7f) }
            val pathMeasure = PathMeasure().apply { setPath(adventurePath, false) }
            val indicatorPos = pathMeasure.getPosition(pathMeasure.length * animatedProgress)
            
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(skyColors))
                
                // Nubes o Estrellas con movimiento
                if (isDark) {
                    val random = java.util.Random(42)
                    repeat(20) { 
                        val rx = (random.nextFloat() * width + cloudOffset * 0.5f) % width
                        val ry = random.nextFloat() * (height * 0.5f)
                        drawCircle(Color.White.copy(alpha = 0.3f), 1.dp.toPx(), androidx.compose.ui.geometry.Offset(rx, ry))
                    }
                    drawCircle(Color(0xFFF5F5F5), 18.dp.toPx(), androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.2f))
                } else {
                    // Nubes dinámicas
                    repeat(3) { i ->
                        val cx = ((width * 0.3f * i) + cloudOffset) % (width + 200f) - 100f
                        val cy = height * (0.15f + i * 0.05f)
                        drawCircle(Color.White.copy(alpha = 0.6f), 20.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(Color.White.copy(alpha = 0.6f), 15.dp.toPx(), androidx.compose.ui.geometry.Offset(cx - 15.dp.toPx(), cy + 5.dp.toPx()))
                        drawCircle(Color.White.copy(alpha = 0.6f), 15.dp.toPx(), androidx.compose.ui.geometry.Offset(cx + 15.dp.toPx(), cy + 5.dp.toPx()))
                    }
                    drawCircle(Color(0xFFFFEB3B), 22.dp.toPx(), androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.2f))
                }

                when (settings.landscape) {
                    LandscapeType.MOUNTAIN -> {
                        // Capa lejana
                        val mPathBack = Path().apply {
                            moveTo(width * 0.4f, height * 0.9f)
                            lineTo(width * 0.65f, height * 0.5f)
                            lineTo(width * 0.9f, height * 0.9f)
                        }
                        drawPath(mPathBack, elementColor.copy(alpha = 0.4f))
                        
                        // Capa cercana
                        val mPathFront = Path().apply {
                            moveTo(width * 0.1f, height * 0.95f)
                            lineTo(width * 0.4f, height * 0.45f)
                            lineTo(width * 0.7f, height * 0.95f)
                        }
                        drawPath(mPathFront, elementColor)
                        
                        val peak = Path().apply {
                            moveTo(width * 0.4f, height * 0.45f)
                            lineTo(width * 0.35f, height * 0.55f)
                            lineTo(width * 0.45f, height * 0.55f)
                        }
                        drawPath(peak, Color.White.copy(alpha = 0.95f))
                    }
                    LandscapeType.FOREST -> {
                        repeat(6) { i ->
                            val tx = width * (0.1f + i * 0.16f)
                            val ty = height * (0.55f + (i % 2) * 0.05f)
                            drawRect(Color(0xFF3E2723), androidx.compose.ui.geometry.Offset(tx - 2.dp.toPx(), ty), androidx.compose.ui.geometry.Size(4.dp.toPx(), height * 0.4f))
                            drawCircle(elementColor, 22.dp.toPx(), androidx.compose.ui.geometry.Offset(tx, ty))
                            drawCircle(elementColor.copy(alpha = 0.7f), 18.dp.toPx(), androidx.compose.ui.geometry.Offset(tx, ty - 12.dp.toPx()))
                        }
                    }
                    LandscapeType.DESERT -> {
                        drawPath(Path().apply {
                            moveTo(0f, height * 0.85f)
                            quadraticTo(width * 0.3f, height * 0.6f, width * 0.6f, height * 0.85f)
                            lineTo(width, height); lineTo(0f, height)
                        }, elementColor.copy(alpha = 0.6f))
                        drawPath(Path().apply {
                            moveTo(width * 0.4f, height * 0.9f)
                            quadraticTo(width * 0.75f, height * 0.65f, width, height * 0.9f)
                            lineTo(width, height); lineTo(width * 0.4f, height)
                        }, elementColor)
                    }
                    LandscapeType.VOLCANO -> {
                        val vPath = Path().apply {
                            moveTo(width * 0.2f, height * 0.95f)
                            lineTo(width * 0.5f, height * 0.4f)
                            lineTo(width * 0.8f, height * 0.95f)
                        }
                        drawPath(vPath, elementColor)
                        val lava = Path().apply {
                            moveTo(width * 0.46f, height * 0.46f)
                            lineTo(width * 0.5f, height * 0.4f)
                            lineTo(width * 0.54f, height * 0.46f)
                            quadraticTo(width * 0.5f, height * 0.5f, width * 0.46f, height * 0.46f)
                        }
                        drawCircle(Color(0xFFFF5722).copy(alpha = 0.3f), 15.dp.toPx(), androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.45f))
                        drawPath(lava, Color(0xFFFF5722))
                    }
                }
                drawPath(adventurePath, Color.White.copy(alpha = 0.4f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                for (i in 0..totalCount) {
                    val f = if (totalCount > 0) i.toFloat() / totalCount else 0f
                    drawCircle(if (i <= completedCount) GoldReward else Color.White.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = pathMeasure.getPosition(pathMeasure.length * f))
                }
            }
            // Indicador con rebote
            Box(Modifier.align(Alignment.TopStart).padding(start = (indicatorPos.x / density).dp - 15.dp, top = (indicatorPos.y / density).dp - 35.dp - bounceValue.dp).size(32.dp).clip(CircleShape).background(Color.White).padding(2.dp).background(PrimaryPurple, CircleShape), Alignment.Center) {
                Text(settings.avatarEmoji, Modifier.graphicsLayer(scaleX = -1f), fontSize = 16.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(if (settings.language == Language.ES) "Progreso de Aventura" else "Adventure Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val mapPhrases = if (settings.language == Language.ES) listOf("¡Tu aventura comienza!", "¡Sigue así!", "¡Meta alcanzada!") else listOf("Adventure starts!", "Keep it up!", "Goal reached!")
                val phrase = when { completedCount == 0 -> mapPhrases[0]; completedCount == totalCount && totalCount > 0 -> mapPhrases.last(); else -> mapPhrases[1] }
                Text(phrase, style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
            }
            Text("${(progressFraction * 100).toInt()}%", color = PrimaryPurple, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: com.angelina.daytask.data.UserEntity?,
    settings: UserSettings, 
    onSettingsChange: (UserSettings) -> Unit, 
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val isDark = settings.darkTheme
    Scaffold(containerColor = Color.Transparent, topBar = { CenterAlignedTopAppBar(title = { Text(if (settings.language == Language.ES) "Ajustes" else "Settings", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState())
                .padding(24.dp), 
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection("Tema", isDark) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                    ThemeOption("Claro", !settings.darkTheme) { onSettingsChange(settings.copy(darkTheme = false)) }
                    ThemeOption("Oscuro", settings.darkTheme) { onSettingsChange(settings.copy(darkTheme = true)) }
                }
            }
            SettingsSection("Paisaje", isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LandscapeType.entries.forEach { type ->
                        val isLocked = type == LandscapeType.VOLCANO && (user?.level ?: 1) < 2
                        LandscapeOption(
                            label = type.name, 
                            selected = settings.landscape == type,
                            locked = isLocked
                        ) { 
                            onSettingsChange(settings.copy(landscape = type)) 
                        }
                    }
                }
            }
            SettingsSection("Avatar", isDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("🏃", "🚴", "🛹", "🚀", "🛸").forEach { emoji ->
                        val isSelected = settings.avatarEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryPurple else Color.Transparent)
                                .border(
                                    1.dp, 
                                    if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSettingsChange(settings.copy(avatarEmoji = emoji)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text(if (settings.language == Language.ES) "Cerrar Sesión" else "Logout", fontWeight = FontWeight.Bold)
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
fun LandscapeOption(label: String, selected: Boolean, locked: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !locked, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = !locked)
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (locked) "$label (🔒 Nivel 2)" else label, 
            color = if (locked) TextGray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ThemeOption(label, selected, onClick)
}

@Composable
fun PatternLockView(onPatternComplete: (String) -> Unit) {
    var currentPath by remember { mutableStateOf<List<Int>>(emptyList()) }
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    
    val sizePx = with(density) { 280.dp.toPx() }
    val step = sizePx / 4
    
    val dotOffsets = remember(sizePx) {
        (0..2).flatMap { y ->
            (0..2).map { x ->
                Offset((x + 1) * step, (y + 1) * step)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .size(280.dp)
            .pointerInput(dotOffsets) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = emptyList()
                        touchPos = offset
                    },
                    onDrag = { change, _ ->
                        touchPos = change.position
                        dotOffsets.forEachIndexed { index, dotOffset ->
                            val dist = (change.position - dotOffset).getDistance()
                            if (dist < 40f && index !in currentPath) {
                                currentPath = currentPath + index
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentPath.size >= 3) {
                            onPatternComplete(currentPath.joinToString(""))
                        }
                        currentPath = emptyList()
                        touchPos = null
                    }
                )
            }
    ) {
        // Draw Dots
        dotOffsets.forEachIndexed { index, offset ->
            val isSelected = index in currentPath
            drawCircle(
                color = if (isSelected) PrimaryPurple else Color.Gray.copy(alpha = 0.3f),
                radius = if (isSelected) 12.dp.toPx() else 8.dp.toPx(),
                center = offset
            )
        }
        
        // Draw Path
        if (currentPath.isNotEmpty()) {
            val path = Path().apply {
                val start = dotOffsets[currentPath[0]]
                moveTo(start.x, start.y)
                for (i in 1 until currentPath.size) {
                    val end = dotOffsets[currentPath[i]]
                    lineTo(end.x, end.y)
                }
                touchPos?.let { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = PrimaryPurple.copy(alpha = 0.5f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )
        }
    }
}
