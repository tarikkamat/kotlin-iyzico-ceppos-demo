package com.iyzico.ceppos.ui.screens

import UserListViewModel
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iyzico.ceppos.utils.loadCredentials

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun UserListScreen(
    modifier: Modifier = Modifier,
    context: Context,
    viewModel: UserListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = UserListViewModelFactory(context)
    )
){
    /** Header Params: Start */
    val (env, apiKey, secretKey, merchantId) = loadCredentials(context)
    /** Header Params: End */

    val userList = remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    val errorMessage = remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                viewModel.fetchUserList(
                    env = env,
                    apiKey = apiKey,
                    secretKey = secretKey,
                    merchantId = merchantId,
                    onError = { errorMessage.value = it }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kullanıcıları Çek")
        }

        if (errorMessage.value.isNotEmpty()) {
            Text(
                text = "Hata: ${errorMessage.value}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.userList.value.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("E-posta", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("İşlem Yetkisi", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
            LazyColumn {
                items(viewModel.userList.value) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = user.first,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (user.second) "Evet" else "Hayır",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

class UserListViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserListViewModel::class.java)) {
            return UserListViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}