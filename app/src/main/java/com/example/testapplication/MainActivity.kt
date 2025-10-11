package com.example.testapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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

    if (isLoggedIn && isAdmin) {
        AdminNavigation(navController = navController, startDestination = startDestination)
    } else {
        StudentNavigation(navController = navController, startDestination = startDestination)
    }
}

@Composable
fun StudentNavigation(navController: NavHostController, startDestination: String) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screensWithBottomBar = listOf("home", "network", "general_interface", "profile")

    Scaffold(
        bottomBar = {
            if (currentRoute in screensWithBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                        sessionManager = SessionManager(LocalContext.current),
                        userId = userId
                    )
                }
            }
            // UPDATED: Passed navController to GeneralInterfaceScreen
            composable("general_interface") { GeneralInterfaceScreen(navController = navController) }

            // NEW ROUTE: For viewing the full post by ID (UUID)
            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") {
                    type = NavType.StringType // UUID is passed as a String
                })
            ) { backStackEntry ->
                val postIdString = backStackEntry.arguments?.getString("postId")
                if (postIdString != null) {
                    PostDetailScreen(navController = navController, postId = UUID.fromString(postIdString))
                }
            }

            composable("home") { HomeScreen(navController = navController) }
            composable("network") { NetworkScreen() }
            composable("profile") { ProfileScreen(navController = navController) }
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
        }
    }
}

@Composable
fun AdminNavigation(navController: NavHostController, startDestination: String) {
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // The admin's home is the only screen they can access
            composable("admin_home") {
                AdminGeneralInterfaceScreen(navController = navController)
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

// --- Simple Screen Composable definitions ---

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
            if (!result.isValid) {
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
    // ... Placeholder for your full SignUpScreen UI and logic ...
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Sign Up", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SetupAccountScreen(navController: NavController, userId: UUID) {
    // ... Placeholder for your full SetupAccountScreen UI and logic ...
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Setup Account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileSetupScreen(navController: NavController, viewModel: ProfileSetupViewModel, sessionManager: SessionManager, userId: UUID) {
    // ... Placeholder for your full ProfileSetupScreen UI and logic ...
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Setup Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}