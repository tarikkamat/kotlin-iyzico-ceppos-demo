package com.iyzico.ceppos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyzico.ceppos.ui.navigation.BottomNavigationBar
import com.iyzico.ceppos.ui.screens.CancelScreen
import com.iyzico.ceppos.ui.screens.PaymentScreen
import com.iyzico.ceppos.ui.screens.SettingsScreen
import com.iyzico.ceppos.ui.screens.UserListScreen
import com.iyzico.ceppos.ui.screens.UserListViewModelFactory
import com.iyzico.ceppos.ui.theme.CepposTheme
import com.iyzico.ceppos.utils.loadCredentials

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CepposTheme {
                val selectedItem = remember { mutableStateOf("settings") }
                val context = this
                val userListViewModel: UserListViewModel =
                    viewModel(factory = UserListViewModelFactory(context))
                val userList by userListViewModel.userList.collectAsState()

                Scaffold(
                    topBar = { TopAppBar(title = { Text("Ceppos Demo") }) },
                    bottomBar = {
                        BottomNavigationBar(
                            selectedItem = selectedItem.value,
                            onItemSelected = { selectedItem.value = it }
                        )
                    },
                    content = { innerPadding ->
                        val (env, apiKey, secretKey, merchantId) = loadCredentials(this)
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            context = this,
                            selectedItem = selectedItem.value,
                            initialEnv = env,
                            initialApiKey = apiKey,
                            initialSecretKey = secretKey,
                            initialMerchantId = merchantId,
                            userList = userList,
                            userListViewModel = userListViewModel,
                            onChangeTab = { newTab ->
                                selectedItem.value = newTab
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    context: Context,
    selectedItem: String,
    initialEnv: String,
    initialApiKey: String,
    initialSecretKey: String,
    initialMerchantId: String,
    userList: List<Pair<String, Boolean>>,
    userListViewModel: UserListViewModel,
    onChangeTab: (String) -> Unit
) {
    when (selectedItem) {
        "settings" -> SettingsScreen(
            modifier = modifier,
            context = context,
            initialEnv = initialEnv,
            initialApiKey = initialApiKey,
            initialSecretKey = initialSecretKey,
            initialMerchantId = initialMerchantId
        )

        "users" -> UserListScreen(
            modifier = modifier,
            context = context,
            viewModel = userListViewModel
        )

        "payment" -> PaymentScreen(
            modifier = modifier,
            context = context,
            userList = userList,
            viewModel = userListViewModel
        )

        "cancel_refund" -> CancelScreen(
            modifier = modifier,
            context = context,
            userList = userList,
            viewModel = userListViewModel
        )

        else -> Text("Seçim geçerli değil.", modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CepposTheme {
        MainScreen(
            modifier = Modifier,
            context = android.content.ContextWrapper(null),
            selectedItem = "settings",
            initialEnv = "Sandbox",
            initialApiKey = "",
            initialSecretKey = "",
            initialMerchantId = "",
            userList = listOf(),
            userListViewModel = viewModel(
                factory = UserListViewModelFactory(
                    android.content.ContextWrapper(
                        null
                    )
                )
            ),
            onChangeTab = { }
        )
    }
}