package com.iyzico.ceppos.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun PaymentScreen(
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

    val amount = remember { mutableStateOf("") }
    val selectedEmail = remember { mutableStateOf(userList.firstOrNull()?.first ?: "") }
    val paymentSource = remember { mutableStateOf("iyzico") }

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
            value = amount.value,
            onValueChange = { amount.value = it },
            label = { Text("Tutar") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paymentSource.value,
            onValueChange = { paymentSource.value = it },
            label = { Text("Ödeme Kaynağı") },
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
                if (amount.value.isBlank() || selectedEmail.value.isBlank() || paymentSource.value.isBlank()) {
                    Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                val amountValue = amount.value.toDoubleOrNull()
                if (amountValue == null || amountValue <= 0) {
                    Toast.makeText(context, "Tutar pozitif bir sayı olmalıdır.", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                scope.launch {
                    createPayment(
                        context = context,
                        env = env,
                        apiKey = apiKey,
                        secretKey = secretKey,
                        merchantId = merchantId,
                        callbackUrl = callbackUrl,
                        amount = amount.value,
                        email = selectedEmail.value,
                        paymentSource = paymentSource.value,
                        onError = { errorMessage.value = it }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ödemeyi Başlat")
        }
    }
}


fun redirectToPaymentGateway(context: android.content.Context, deepLinkUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

suspend fun createPayment(
    context: android.content.Context,
    env: String,
    apiKey: String,
    secretKey: String,
    merchantId: String,
    callbackUrl: String,
    amount: String,
    email: String,
    paymentSource: String,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()
    val jsonBody = JSONObject().apply {
        put("amount", amount)
        put("email", email)
        put("paymentSource", paymentSource)
    }.toString()

    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val baseUrl =
        if (env == "Sandbox") "https://sandbox-api.iyzipay.com" else "https://api.iyzipay.com"
    val request = Request.Builder()
        .url("$baseUrl/v2/in-store/payment")
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