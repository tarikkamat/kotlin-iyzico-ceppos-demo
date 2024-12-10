package com.iyzico.ceppos.utils

import android.content.Context

fun saveCredentials(context: Context, env: String, apiKey: String, secretKey: String, merchantId: String) {
    val sharedPreferences = context.getSharedPreferences("CepposPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("ENV", env)
    editor.putString("API_KEY", apiKey)
    editor.putString("SECRET_KEY", secretKey)
    editor.putString("MERCHANT_ID", merchantId)
    editor.apply()
}

fun loadCredentials(context: Context): Credentials {
    val sharedPreferences = context.getSharedPreferences("CepposPrefs", Context.MODE_PRIVATE)
    val env = sharedPreferences.getString("ENV", "") ?: ""
    val apiKey = sharedPreferences.getString("API_KEY", "") ?: ""
    val secretKey = sharedPreferences.getString("SECRET_KEY", "") ?: ""
    val merchantId = sharedPreferences.getString("MERCHANT_ID", "") ?: ""
    return Credentials(env, apiKey, secretKey, merchantId)
}

data class Credentials(
    val env: String,
    val apiKey: String,
    val secretKey: String,
    val merchantId: String
)