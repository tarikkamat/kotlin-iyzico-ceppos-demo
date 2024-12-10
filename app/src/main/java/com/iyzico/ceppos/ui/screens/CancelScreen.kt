package com.iyzico.ceppos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iyzico.ceppos.utils.loadCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelScreen(
    modifier: Modifier = Modifier,
    context: android.content.Context,
    userList: List<Pair<String, Boolean>>,
    viewModel: UserListViewModel,
) {
    if (userList.isEmpty()) {
        EmptyUserScreen()
        return
    }

    val (env, apiKey, secretKey, merchantId) = loadCredentials(context)
    val errorMessage = remember { mutableStateOf("") }
    val callbackUrl = "myapp://payment/callback"

    val refundAmount = remember { mutableStateOf("") }
    val paymentId = remember { mutableStateOf("") }
    val selectedEmail = remember { mutableStateOf(userList.firstOrNull()?.first ?: "") }

    val scope = rememberCoroutineScope()
    var emailDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = emailDropdownExpanded,
            onExpandedChange = { emailDropdownExpanded = !emailDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedEmail.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Hesap") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(emailDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = emailDropdownExpanded,
                onDismissRequest = { emailDropdownExpanded = false }
            ) {
                userList.forEach { user ->
                    DropdownMenuItem(
                        text = { Text(user.first) },
                        onClick = {
                            selectedEmail.value = user.first
                            emailDropdownExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = refundAmount.value,
            onValueChange = { refundAmount.value = it },
            label = { Text("İade Tutar") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paymentId.value,
            onValueChange = { paymentId.value = it },
            label = { Text("Ödeme Numarası") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.value.isNotEmpty()) {
            Text(
                text = "Hata: ${errorMessage.value}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                if (refundAmount.value.isBlank() || selectedEmail.value.isBlank() || paymentId.value.isBlank()) {
                    Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                val amountValue = refundAmount.value.toDoubleOrNull()
                if (amountValue == null || amountValue <= 0) {
                    Toast.makeText(context, "Tutar pozitif bir sayı olmalıdır.", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                scope.launch {
                    createRefund(
                        context = context,
                        env = env,
                        apiKey = apiKey,
                        secretKey = secretKey,
                        merchantId = merchantId,
                        callbackUrl = callbackUrl,
                        refundAmount = refundAmount.value,
                        paymentId = paymentId.value,
                        email = selectedEmail.value,
                        onError = { errorMessage.value = it }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("İadeyi Başlat")
        }

    }
}


suspend fun createRefund(
    context: android.content.Context,
    env: String,
    apiKey: String,
    secretKey: String,
    merchantId: String,
    callbackUrl: String,
    refundAmount: String,
    paymentId: String,
    email: String,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()
    val jsonBody = JSONObject().apply {
        put("refundAmount", refundAmount)
        put("paymentId", paymentId)
        put("email", email)
    }.toString()

    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val baseUrl =
        if (env == "Sandbox") "https://sandbox-api.iyzipay.com" else "https://api.iyzipay.com"
    val request = Request.Builder()
        .url("$baseUrl/v2/in-store/payment/refund")
        .post(requestBody)
        .addHeader("x-api-key", apiKey)
        .addHeader("x-secret-key", secretKey)
        .addHeader("x-merchant-id", merchantId)
        .addHeader("x-callback-url", callbackUrl)
        .addHeader("x-iyzi-rnd", System.currentTimeMillis().toString())
        .build()

    try {
        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        val responseBody = response.body?.string()
        if (response.isSuccessful && responseBody != null) {
            val json = JSONObject(responseBody)
            if (json.getString("status") == "success") {
                redirectToPaymentGateway(context, json.getString("deepLinkUrl"))
            } else {
                val errorMessage = json.optString("errorMessage", "Bilinmeyen bir hata oluştu.")
                onError(errorMessage)
            }
        } else {
            onError("API çağrısı başarısız oldu: ${response.code}")
        }
    } catch (e: Exception) {
        onError("Bir hata oluştu: ${e.message}")
    }
}