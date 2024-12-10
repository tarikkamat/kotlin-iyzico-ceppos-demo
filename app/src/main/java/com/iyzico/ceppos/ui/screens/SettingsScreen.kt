package com.iyzico.ceppos.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iyzico.ceppos.utils.saveCredentials
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    context: Context,
    initialEnv: String,
    initialApiKey: String,
    initialSecretKey: String,
    initialMerchantId: String
) {
    var env by remember { mutableStateOf(initialEnv) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var secretKey by remember { mutableStateOf(initialSecretKey) }
    var merchantId by remember { mutableStateOf(initialMerchantId) }

    val environments = listOf("Sandbox", "Live")
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = env,
                onValueChange = {},
                readOnly = true,
                label = { Text("Ortam") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                environments.forEach { environment ->
                    DropdownMenuItem(
                        text = { Text(environment) },
                        onClick = {
                            env = environment
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Anahtarı") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = secretKey,
            onValueChange = { secretKey = it },
            label = { Text("Güvenlik Anahtarı") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = merchantId,
            onValueChange = { merchantId = it },
            label = { Text("Üye İşyeri Numarası") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                saveCredentials(context, env, apiKey, secretKey, merchantId)
                Toast.makeText(context, "Ayarlar Kaydedildi", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kaydet")
        }
    }
}