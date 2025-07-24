package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.ui.composables.SearchResult
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Search(dataViewModel: DataViewModel) {
    var searchQuery by remember { mutableStateOf("") }

    val gradientBackground = Brush.verticalGradient(
        listOf(Color(0xFF0F0F0F), Color.Black)
    )

    val dummyResults = dataViewModel.songs.collectAsState().value.filter {
        it.title.contains(searchQuery, ignoreCase = true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(gradientBackground)
    ) {
        Row (
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
//            .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.03f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Transparent
                        ),

                        )
                )
                .padding(17.dp, 4.dp)
        ){

        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {


            // Title / Heading
            Text(
                text = "Search Music",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search songs, artists...", color = Color.Gray)
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
                },
                shape = RoundedCornerShape(25.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(25.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filters (optional for future)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                listOf("All", "WorkOutPump", "Relax").forEach { filter ->
                    FilterChip(
                        selected = false,
                        onClick = { /* handle filter */ },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dummyResults) { result ->
                    SearchResult(result){
                        dataViewModel.playSong(context,result)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(132.dp))
                }

                if (dummyResults.isEmpty()) {
                    item {
                        Text(
                            "No results found.",
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
