package com.example.testapplication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Represents an interest with a name and rating
data class Interest(val name: String, val rating: Int)

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
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