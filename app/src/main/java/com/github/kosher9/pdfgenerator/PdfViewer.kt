package com.github.kosher9.pdfgenerator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PdfViewer() {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.Green),
            content = {})
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(60.dp)
                .align(Alignment.BottomCenter),
            contentColor = Color.Black,
        ){
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "01 / 05 Pages",
                    modifier = Modifier
                        .background(
                            color = Color(0xFFF0F5F6),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(40.dp, 8.dp)
                )
            }
        }
    }
}

@Composable
fun BottomBarLayout() {
    var text by remember { mutableStateOf("Hello, Jetpack Compose!") }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Your main content goes here
            Text(text, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* Handle button click */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Click me")
            }
        }

        // Fixed bottom bar
        BottomAppBar(
            ) {
            // Add your bottom bar content here
            IconButton(
                onClick = { /* Handle icon click */ }
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { /* Handle icon click */ }
            ) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = null)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { /* Handle icon click */ }
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
fun PdfViewerPreview() {
    PdfViewer()
}