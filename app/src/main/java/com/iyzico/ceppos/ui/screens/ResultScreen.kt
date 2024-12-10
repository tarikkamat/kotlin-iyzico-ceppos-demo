package com.iyzico.ceppos.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iyzico.ceppos.MainActivity
import com.iyzico.ceppos.utils.loadCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

@Composable
fun ResultScreen(
    modifier: Modifier = Modifier,
    context: Context,
    data: String,
    paymentSessionToken: String
) {
    val (env, apiKey, secretKey, merchantId) = loadCredentials(context)
    var resultData by remember { mutableStateOf<JSONObject?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(data, paymentSessionToken) {
        try {
            resultData = decryptPaymentResult(
                context = context,
                env = env,
                apiKey = apiKey,
                secretKey = secretKey,
                merchantId = merchantId,
                data = data,
                paymentSessionToken = paymentSessionToken
            )
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Ödeme sonucu alınamadı"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            resultData?.let { result ->
                SuccessResultContent(result)
            }

            errorMessage?.let { message ->
                ErrorResultContent(message, data, paymentSessionToken)
            }
        }

        Button(
            onClick = {
                context.startActivity(
                    android.content.Intent(context, MainActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Uygulamaya Dön")
        }
    }
}

@Composable
fun SuccessResultContent(result: JSONObject) {
    val transaction = result.getJSONObject("inStoreCompleteOperation").getJSONObject("transaction")
    val receipt = transaction.getJSONObject("receipt")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "İşlem Başarılı",
            tint = Color.Green,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ödeme Onaylandı",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                ReceiptItem("Tutar", "${transaction.getDouble("amount")} TL")
                ReceiptItem("Kart", transaction.getString("maskedPan"))
                ReceiptItem("İşlem Tarihi", transaction.getString("transactionDate"))
                ReceiptItem("Onay Kodu", transaction.getString("authorizationCode"))
            }
        }
    }
}

@Composable
fun ErrorResultContent(message: String, data: String, paymentSessionToken: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Ödeme Başarısız",
            tint = Color.Red,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ödeme Başarısız",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        val context = LocalContext.current
        val copyData = "data: $data\npaymentSessionToken: $paymentSessionToken"

        Button(
            onClick = {
                copyToClipboard(context, copyData)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Callback Verilerini Kopyala")
        }

        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
fun ReceiptItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            modifier = Modifier.weight(2f)
        )
    }
}

suspend fun decryptPaymentResult(
    context: Context,
    env: String,
    apiKey: String,
    secretKey: String,
    merchantId: String,
    data: String,
    paymentSessionToken: String
): JSONObject = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val jsonBody = JSONObject().apply {
        put("data", data)
        put("paymentSessionToken", paymentSessionToken)
    }.toString()

    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val baseUrl =
        if (env == "Sandbox") "https://sandbox-api.iyzipay.com" else "https://api.iyzipay.com"
    val request = Request.Builder()
        .url("$baseUrl/v2/in-store/crypt/decrypt")
        .post(requestBody)
        .addHeader("x-api-key", apiKey)
        .addHeader("x-secret-key", secretKey)
        .addHeader("x-merchant-id", merchantId)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Sunucu hatası: ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw IOException("Boş yanıt")
        JSONObject(responseBody)
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("Callback Data", text)
    clipboardManager.setPrimaryClip(clipData)

    Toast.makeText(context, "Veriler kopyalandı", Toast.LENGTH_SHORT).show()
}