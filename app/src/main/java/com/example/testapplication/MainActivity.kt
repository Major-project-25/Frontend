package com.example.testapplication

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.testapplication.ui.theme.TestapplicationTheme
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt
import androidx.activity.enableEdgeToEdge

data class Interest(val name: String, val rating: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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

// In MainActivity.kt

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val isLoggedIn by sessionManager.isLoggedInFlow.collectAsState(initial = false)
    val startDestination = if (isLoggedIn) "home" else "splash"

    // 1. Get the current screen's route from the NavController
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 2. Define the list of screens that should have the bottom bar
    val screensWithBottomBar = listOf("home", "network", "general_interface", "profile")

    Scaffold(
        bottomBar = {
            // 3. Only show the BottomNavigationBar if the current route is in our list
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
            // All your composable destinations (like "splash", "signin", "home", etc.)
            // remain exactly the same here.
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
                } else {
                    navController.popBackStack()
                }
            }
            composable(
                "profile_setup/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.let { UUID.fromString(it) }
                if (userId != null) {
                    val profileViewModel: ProfileSetupViewModel = viewModel()
                    ProfileSetupScreen(
                        navController = navController,
                        viewModel = profileViewModel,
                        sessionManager = sessionManager,
                        userId = userId
                    )
                } else {
                    navController.popBackStack()
                }
            }
            composable("home") {
                HomeScreen(navController = navController)
            }
            composable("network") {
                NetworkScreen()
            }
            composable("general_interface") {
                GeneralInterfaceScreen()
            }

            composable("requests") {
                RequestsScreen(navController = navController)
            }

            // In your NavHost inside AppNavigation()

            composable(
                "chat/{usn}",
                arguments = listOf(navArgument("usn") { type = NavType.StringType })
            ) { backStackEntry ->
                val usn = backStackEntry.arguments?.getString("usn")
                if (usn != null) {
                    ChatScreen(navController = navController, friendUsn = usn)
                }
            }
            composable("profile") {
                // Replace the old placeholder with this new function call
                ProfileScreen(navController = navController)
            }

            composable("profile") {
                ProfileScreen(navController = navController)
            }

            composable("edit_profile") {
                EditProfileScreen(navController = navController)
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { KycLogo() }
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
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
            if (result.isValid && result.userId != null) {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {
                Toast.makeText(context, "Invalid email or password.", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearResult()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KycLogo()
        Text("Sign in", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") },
            placeholder = { Text("college email id") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signInUser(UserCreate(email, password))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Text("Next", modifier = Modifier.padding(vertical = 8.dp))
        }
        OrDivider()
        Button(
            onClick = { navController.navigate("signup") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Text("Create account", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val authResult by viewModel.authResult.collectAsState()

    LaunchedEffect(authResult) {
        authResult?.let { result ->
            if (result.isValid && result.userId != null) {
                Toast.makeText(context, "Sign up successful!", Toast.LENGTH_SHORT).show()
                navController.navigate("account_setup/${result.userId}") {
                    popUpTo("welcome") { inclusive = false }
                }
            } else {
                Toast.makeText(context, "Sign up failed. Email may already exist.", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearResult()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KycLogo()
        Text("Sign up", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") },
            placeholder = { Text("college email id") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                } else if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signUpUser(UserCreate(email, password))
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Text("Next", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun SetupAccountScreen(navController: NavController, userId: UUID) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Let's set up your\nAccount",
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("profile_setup/$userId") },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileSetupViewModel,
    sessionManager: SessionManager,
    userId: UUID
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageUriChange(uri)
    }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    val validationError = viewModel.validateProfile()
                    if (validationError != null) {
                        Toast.makeText(context, validationError, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateUserProfile(userId, sessionManager)
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
            ) {
                Text("Next", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = viewModel.imageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray).clickable { imagePickerLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_launcher_foreground)
                    )
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Picture",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF0077FF)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = viewModel.usn,
                    onValueChange = { viewModel.usn = it },
                    label = { Text("USN") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = viewModel.bio,
                    onValueChange = { viewModel.bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(120.dp),
                    maxLines = 5
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }

            item { Text("Rate Your Interests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth()) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    viewModel.interests.forEachIndexed { index, interest ->
                        Box(modifier = Modifier.width(280.dp)) {
                            InterestDropdownAndSlider(
                                availableInterests = viewModel.predefinedInterests.filter { interestName ->
                                    interestName == "None" || viewModel.interests.none { it.name == interestName } || viewModel.interests[index].name == interestName
                                },
                                selectedInterest = interest,
                                onInterestChange = { newInterest ->
                                    val newInterests = viewModel.interests.toMutableList()
                                    newInterests[index] = newInterest
                                    viewModel.interests = newInterests
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}


@Composable
fun KycLogo() {
    Text(
        text = "KYC", color = Color(0xFF0077FF), fontSize = 60.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text("or", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Gray)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestDropdownAndSlider(
    availableInterests: List<String>,
    selectedInterest: Interest,
    onInterestChange: (Interest) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedInterest.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    )
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    availableInterests.forEach { interestName ->
                        DropdownMenuItem(
                            text = { Text(interestName) },
                            onClick = {
                                val newRating = if (interestName == "None") 0 else selectedInterest.rating
                                onInterestChange(Interest(name = interestName, rating = newRating))
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = selectedInterest.rating.toFloat(),
                    onValueChange = {
                        onInterestChange(selectedInterest.copy(rating = it.roundToInt()))
                    },
                    valueRange = 0f..10f,
                    steps = 9,
                    modifier = Modifier.weight(1f),
                    enabled = selectedInterest.name != "None"
                )
                Text(
                    text = selectedInterest.rating.toString(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TestapplicationTheme {
        val navController = rememberNavController()
        ProfileSetupScreen(navController, viewModel(), SessionManager(LocalContext.current), UUID.randomUUID())
    }
}