package com.example.testapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testapplication.ui.theme.TestapplicationTheme
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import java.net.URLDecoder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ExitToApp // Added for Logout
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        NotificationService.createNotificationChannel(this)
        setContent {
            TestapplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val isLoggedIn by sessionManager.isLoggedInFlow.collectAsState(initial = false)
    val isAdmin by sessionManager.isAdminFlow.collectAsState(initial = false)

    val startDestination = when {
        isLoggedIn && isAdmin -> "admin_home"
        isLoggedIn && !isAdmin -> "home"
        else -> "splash"
    }

    // FIX: Simplified the top-level logic to use a single NavHost
    // We will handle the different UIs (Admin vs Student) inside the Scaffold/NavHost structure below.

    // We must define the Student's navigation graph first for the BottomBar check
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screensWithStudentBottomBar = listOf("home", "network", "general_interface", "profile")

    // The student's bottom bar logic goes here.
    Scaffold(
        bottomBar = {
            if (currentRoute in screensWithStudentBottomBar) {
                BottomNavigationBar(navController = navController)
            }
            // Admin navigation has its own Scaffold/BottomBar via AdminHostScreen
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Common/Auth Routes ---
            composable("splash") { SplashScreen(navController = navController) }
            composable("welcome") { WelcomeScreen(navController = navController) }
            composable("signin") {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(SessionManager(LocalContext.current))
                )
                SignInScreen(navController = navController, viewModel = authViewModel)
            }
            composable("signup") {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(SessionManager(LocalContext.current))
                )
                SignUpScreen(navController = navController, viewModel = authViewModel)
            }
            composable(
                "account_setup/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.let { UUID.fromString(it) }
                if (userId != null) {
                    SetupAccountScreen(navController = navController, userId = userId)
                }
            }
            composable(
                "profile_setup/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.let { UUID.fromString(it) }
                if (userId != null) {
                    val profileSetupViewModel: ProfileSetupViewModel = viewModel()
                    ProfileSetupScreen(
                        navController = navController,
                        viewModel = profileSetupViewModel,
                        sessionManager = sessionManager,
                        userId = userId
                    )
                }
            }
            // --- Student Primary Routes ---
            composable("general_interface") { GeneralInterfaceScreen(navController = navController) }
            composable("home") { HomeScreen(navController = navController) }
            composable("network") { NetworkScreen() }
            composable("profile") { ProfileScreen(navController = navController) }

            // --- Secondary/Common Routes ---
            composable("edit_profile") { EditProfileScreen(navController = navController) }
            composable("requests") { RequestsScreen(navController = navController) }
            composable(
                "chat/{name}/{userId}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: "User"
                val userIdString = backStackEntry.arguments?.getString("userId")
                if (userIdString != null) {
                    ChatScreen(
                        navController = navController,
                        friendName = name,
                        friendId = UUID.fromString(userIdString)
                    )
                }
            }
            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val postIdString = backStackEntry.arguments?.getString("postId")
                if (postIdString != null) {
                    PostDetailScreen(navController = navController, postId = UUID.fromString(postIdString))
                }
            }
            composable(
                route = "media_viewer/{encodedUrl}",
                arguments = listOf(navArgument("encodedUrl") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl")
                if (encodedUrl != null) {
                    val fullUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                    FullScreenMediaViewer(navController = navController, mediaUrl = fullUrl)
                }
            }

            // --- Admin Top-Level Route ---
            // The destination for admin_home is now the new Host Screen with the bottom tabs
            composable("admin_home") {
                AdminHostScreen(navController = navController)
            }
        }
    }
}

// REMOVED AdminNavigation and StudentNavigation functions as they are consolidated above.
// The code below defines the new AdminHostScreen.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHostScreen(navController: NavHostController) {
    // NavController for the inner host to switch between Create and Feed
    val innerNavController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val adminItems = listOf(
        NavigationItem("Create", "admin_create", Icons.Default.Upload),
        NavigationItem("Feed", "admin_feed", Icons.Default.Search)
    )
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            "admin_create" -> "General Info"
                            "admin_feed" -> "Updates & Events"
                            else -> "Admin Interface"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Logout Button for Admin
                    IconButton(onClick = {
                        coroutineScope.launch {
                            sessionManager.setLoggedIn(false)
                            sessionManager.setAdminStatus(false)
                            navController.navigate("splash") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                adminItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            innerNavController.navigate(item.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController, // Use the innerNavController
            startDestination = "admin_create",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Note: AdminCreatePostScreen is the new name for AdminGeneralInterfaceScreen
            composable("admin_create") { AdminCreatePostScreen(navController = navController) }
            composable("admin_feed") { GeneralInterfaceScreen(navController = navController) }

            // Route to Post Details (Navigates using the OUTER navController)
            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val postIdString = backStackEntry.arguments?.getString("postId")
                if (postIdString != null) {
                    PostDetailScreen(navController = navController, postId = UUID.fromString(postIdString))
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem("Home", "home", Icons.Default.Home),
        NavigationItem("Network", "network", Icons.Outlined.Lan),
        NavigationItem("General", "general_interface", Icons.Default.Search),
        NavigationItem("Profile", "profile", Icons.Default.Person)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val title: String, val route: String, val icon: ImageVector)

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2000L)
        navController.navigate("welcome") {
            popUpTo("splash") { inclusive = true }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("KYC", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to KYC !", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "where meaningful connections, collaborations, and conversations begin",
            fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.Gray
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { navController.navigate("signin") },
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun SignInScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val authResult by viewModel.authResult.collectAsState()

    LaunchedEffect(authResult) {
        authResult?.let { result ->
            if (result.isValid) {
                if (result.isAdmin == true) {
                    // Navigate to the new Admin Host screen
                    navController.navigate("admin_home") {
                        popUpTo("signin") { inclusive = true }
                    }
                } else {
                    // Navigate to the regular student home flow
                    if (result.userId != null) {
                        navController.navigate("home") {
                            popUpTo("signin") { inclusive = true }
                        }
                    }
                }
            } else if (result.isValid == false) {
                Toast.makeText(context, "Invalid email or password.", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearResult()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Sign In", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = email, onValueChange = {email = it}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = {password = it}, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { viewModel.signInUser(UserCreate(email, password)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign In")
        }
        TextButton(onClick = { navController.navigate("signup") }) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController, viewModel: AuthViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Sign Up", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SetupAccountScreen(navController: NavController, userId: UUID) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Setup Account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileSetupScreen(navController: NavController, viewModel: ProfileSetupViewModel, sessionManager: SessionManager, userId: UUID) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Setup Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}