package com.iyzico.ceppos.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.iyzico.ceppos.ui.theme.Icon

@Composable
fun BottomNavigationBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedItem == "settings",
            onClick = { onItemSelected("settings") },
            label = { Text("Ayarlar") },
            icon = { Icon(Icon.Settings, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedItem == "users",
            onClick = { onItemSelected("users") },
            label = { Text("Kullanıcılar") },
            icon = { Icon(Icon.Person, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedItem == "payment",
            onClick = { onItemSelected("payment") },
            label = { Text("Ödeme") },
            icon = { Icon(Icon.ShoppingCart, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedItem == "cancel_refund",
            onClick = { onItemSelected("cancel_refund") },
            label = { Text("İptal/İade") },
            icon = { Icon(Icon.Refresh, contentDescription = null) }
        )
    }
}
