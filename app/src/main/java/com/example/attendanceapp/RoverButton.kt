package com.example.attendanceapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoverButton(actions: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Column(horizontalAlignment = Alignment.End) {
            if (expanded) {
                actions.forEach { (label, action) ->
                    Button(
                        onClick = action,
                        modifier = Modifier
                            .padding(4.dp)
                            .width(180.dp)
                    ) {
                        Text(label)
                    }
                }
            }

            FloatingActionButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Rover Menu")
            }
        }
    }
}