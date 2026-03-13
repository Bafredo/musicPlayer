package com.muyoma.thapab.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.muyoma.thapab.ui.theme.ThemeMode


@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    themeMode: ThemeMode,
    onThemeToggle: () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                tint = if(currentRoute == "explore") selectedColor else unselectedColor
            )
            Text(
                text = "Explore",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
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
                tint = if(currentRoute == "liked") selectedColor else unselectedColor
            )
            Text(
                text = "Liked",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
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
                tint = if(currentRoute == "local") selectedColor else unselectedColor

            )
            Text(
                text = "Device",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
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
                tint = if(currentRoute == "search") selectedColor else unselectedColor

            )
            Text(
                text = "Find",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable { onThemeToggle() }
        ) {
            Icon(
                imageVector = when (themeMode) {
                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.DARK -> Icons.Default.DarkMode
                },
                contentDescription = "Theme",
                tint = selectedColor
            )
            Text(
                text = themeMode.label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
