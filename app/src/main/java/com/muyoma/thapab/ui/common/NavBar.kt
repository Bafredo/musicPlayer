package com.muyoma.thapab.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState


@Composable
fun BottomNavigationBar(modifier: Modifier = Modifier,navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    Row(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth()
            .padding(18.dp,0.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable {
                    navController.navigate("explore")
                }
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                contentDescription = "Explore",
                tint = if(currentRoute == "explore") Color(0x9E07F6F6) else Color.White
            )
            Text(
                text = "Explore",
                fontSize = 11.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable {
                    navController.navigate("liked")
                }
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorites",
                tint = if(currentRoute == "liked") Color(0x9E07F6F6) else Color.White
            )
            Text(
                text = "Liked",
                fontSize = 11.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable {
                    navController.navigate("local")
                }

        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = "Local",
                tint = if(currentRoute == "local") Color(0x9E07F6F6) else Color.White

            )
            Text(
                text = "Device",
                fontSize = 11.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable {
                    navController.navigate("search")
                }
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = if(currentRoute == "search") Color(0x9E07F6F6) else Color.White

            )
            Text(
            text = "Find",
            fontSize = 11.sp
            )
        }
    }
}
