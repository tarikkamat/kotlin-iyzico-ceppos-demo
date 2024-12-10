import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class UserListViewModel(private val context: Context) : ViewModel() {
    private val _userList = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val userList: StateFlow<List<Pair<String, Boolean>>> = _userList

    init {
        _userList.value = loadSavedUsers()
    }

    private fun loadSavedUsers(): List<Pair<String, Boolean>> {
        val prefs = context.getSharedPreferences("UserListPrefs", Context.MODE_PRIVATE)
        val userCount = prefs.getInt("userCount", 0)
        val savedUsers = mutableListOf<Pair<String, Boolean>>()

        for (i in 0 until userCount) {
            val email = prefs.getString("user_email_$i", "") ?: ""
            val canPerformAction = prefs.getBoolean("user_action_$i", false)
            if (email.isNotEmpty()) {
                savedUsers.add(email to canPerformAction)
            }
        }

        return savedUsers
    }

    private fun saveUsers(users: List<Pair<String, Boolean>>) {
        val prefs = context.getSharedPreferences("UserListPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("userCount", users.size)
            users.forEachIndexed { index, user ->
                putString("user_email_$index", user.first)
                putBoolean("user_action_$index", user.second)
            }
            apply()
        }
    }

    fun fetchUserList(env: String, apiKey: String, secretKey: String, merchantId: String, onError: (String) -> Unit) {
        val baseUrl = if (env == "Sandbox") "https://sandbox-api.iyzipay.com" else "https://api.iyzipay.com"
        viewModelScope.launch {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$baseUrl/v2/in-store/user-info/list")
                .addHeader("x-api-key", apiKey)
                .addHeader("x-secret-key", secretKey)
                .addHeader("x-merchant-id", merchantId)
                .build()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getString("status") == "success") {
                        val fetchedUsers = mutableListOf<Pair<String, Boolean>>()
                        val users = json.getJSONArray("userInfoList")
                        for (i in 0 until users.length()) {
                            val user = users.getJSONObject(i)
                            val email = user.getString("email")
                            val canPerformAction = user.getBoolean("canPerformAction")
                            fetchedUsers.add(email to canPerformAction)
                        }
                        _userList.value = fetchedUsers
                        saveUsers(fetchedUsers)
                    } else {
                        val errorMessage = json.optString("errorMessage", "Bilinmeyen bir hata oluştu.")
                        onError(errorMessage)
                    }
                } else {
                    onError("API çağrısı başarısız oldu: ${response.code}")
                    _userList.value = emptyList()
                    val prefs = context.getSharedPreferences("UserListPrefs", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                }
            } catch (e: Exception) {
                onError("Bir hata oluştu: ${e.message}")
            }
        }
    }
}