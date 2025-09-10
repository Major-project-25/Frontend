package com.example.testapplication



import android.widget.Toast

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.navigation.NavController

import androidx.navigation.compose.rememberNavController

import com.example.testapplication.ui.theme.TestapplicationTheme



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun HomeScreen(navController: NavController) {

    Column(modifier = Modifier.fillMaxSize()) {

        HomeTopBar(navController = navController)

// The navController is no longer needed by EmptyChatView

        EmptyChatView()

    }

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun HomeTopBar(navController: NavController) {

    CenterAlignedTopAppBar(

        title = {

            Text(

                "Chats",

                fontWeight = FontWeight.Bold,

                fontSize = 28.sp,

                color = Color.White

            )

        },

        actions = {

            IconButton(

                onClick = { navController.navigate("requests") },

                modifier = Modifier

                    .padding(end = 8.dp)

                    .size(48.dp)

            ) {

                Icon(

                    imageVector = Icons.Default.Add,

                    contentDescription = "Add Chat",

                    modifier = Modifier.fillMaxSize(),

                    tint = Color.Black

                )

            }

        },

        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(

            containerColor = Color(0xFF0077FF)

        )

    )

}



// --- UPDATED: The function no longer needs a NavController ---

@Composable

fun EmptyChatView(modifier: Modifier = Modifier) {

    val context = LocalContext.current // Get the context to show a Toast



    Column(

        modifier = modifier.fillMaxSize(),

        verticalArrangement = Arrangement.Center,

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text("Empty chat", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        Button(

// --- UPDATED: This button now shows a Toast message ---

            onClick = {

                Toast.makeText(

                    context,

                    "To connect with students, go to the Network screen.",

                    Toast.LENGTH_LONG

                ).show()

            },

            shape = RoundedCornerShape(50),

            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))

        ) {

            Text(

                "Network",

                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),

                fontSize = 16.sp

            )

        }

    }

}



@Preview(showBackground = true)
@Composable // <-- This annotation was likely missing
fun HomeScreenPreview() {
    TestapplicationTheme { // <-- This now works because of the import
        HomeScreen(navController = rememberNavController())
    }
}