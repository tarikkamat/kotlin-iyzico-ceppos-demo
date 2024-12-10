package com.iyzico.ceppos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.iyzico.ceppos.ui.screens.ErrorScreen
import com.iyzico.ceppos.ui.screens.ResultScreen
import com.iyzico.ceppos.ui.theme.CepposTheme
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri != null) {
            val data = uri.getQueryParameter("data")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.name()).replace(" ", "+")
            }
            val paymentSessionToken = uri.getQueryParameter("paymentSessionToken")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.name())
            }

            if (data != null && paymentSessionToken != null) {
                setContent {
                    CepposTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            ResultScreen(
                                context = this,
                                data = data,
                                paymentSessionToken = paymentSessionToken
                            )
                        }
                    }
                }
            } else {
                Log.e("ResultActivity", "Data or PaymentSessionToken NULL")
                setContent {
                    CepposTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            ErrorScreen(
                                context = this
                            )
                        }
                    }
                }
            }
        } else {
            Log.e("ResultActivity", "URI NULL")
            setContent {
                CepposTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ErrorScreen(
                            context = this
                        )
                    }
                }
            }
        }
    }
}