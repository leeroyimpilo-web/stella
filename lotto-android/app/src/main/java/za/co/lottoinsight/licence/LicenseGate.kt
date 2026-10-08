package za.co.lottoinsight.licence

import android.content.Context
import android.provider.Settings
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import za.co.lottoinsight.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

private val accent = Color(0xFFFFDA00)
private val dark = Color(0xFF12151A)

private class LicenseStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("lotto_paid_licence", Context.MODE_PRIVATE)
    val installationId: String
        get() = prefs.getString("installation_id", null) ?:
            UUID.randomUUID().toString().also {
                prefs.edit().putString("installation_id", it).apply()
            }

    val deviceHash: String
        get() {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unavailable"
            val value = "${context.packageName}:$id".toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
        }

    fun valid(): Boolean {
        val token = prefs.getString("license_token", null) ?: return false
        return verify(token, deviceHash, installationId)
    }

    fun save(token: String, retailer: String) {
        check(verify(token, deviceHash, installationId)) { "Invalid activation signature or device binding" }
        prefs.edit().putString("license_token", token).putString("retailer", retailer).apply()
    }

    private fun verify(token: String, deviceHash: String, installationId: String): Boolean = try {
        if (BuildConfig.LICENSE_PUBLIC_KEY_B64.isBlank()) return false
        val pieces = token.split(".")
        if (pieces.size != 2) return false
        val flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        val publicBytes = Base64.decode(BuildConfig.LICENSE_PUBLIC_KEY_B64, Base64.DEFAULT)
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicBytes))
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(key)
        signature.update(pieces[0].toByteArray(Charsets.UTF_8))
        if (!signature.verify(Base64.decode(pieces[1], flags))) return false
        val payload = JSONObject(String(Base64.decode(pieces[0], flags), Charsets.UTF_8))
        payload.optString("product") == "lotto-intelligence" &&
            payload.optString("deviceHash") == deviceHash &&
            payload.optString("installationId") == installationId &&
            payload.optString("licenseId").isNotBlank()
    } catch (_: Exception) { false }
}

private suspend fun requestActivation(
    serial: String,
    deviceHash: String,
    installationId: String
): Pair<String, String> = withContext(Dispatchers.IO) {
    val base = BuildConfig.LICENSE_SERVER_URL.trimEnd('/')
    if (!base.startsWith("https://") || base.contains("example.invalid"))
        error("The activation server is not configured for this build")
    val connection = URL("$base/api/activate").openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 18000
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json")
    try {
        val body = JSONObject().put("serial", serial.trim().uppercase())
            .put("deviceHash", deviceHash).put("installationId", installationId)
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val successful = connection.responseCode in 200..299
        val response = (if (successful) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: "{}"
        val data = JSONObject(response)
        if (!successful) error(data.optString("error", "Activation failed"))
        val token = data.getString("token")
        val retailer = data.optString("retailer", "Retail Partner")
        token to retailer
    } finally { connection.disconnect() }
}

@Composable
fun LicenseGate(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { LicenseStorage(context) }
    var active by remember { mutableStateOf(storage.valid()) }
    var serial by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    if (active) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().background(dark).padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = accent, shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("L", fontSize = 42.sp, fontWeight = FontWeight.Black, color = dark)
                }
            }
            Spacer(Modifier.height(25.dp))
            Text("Activate Lotto Intelligence", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Enter the serial code received after your R50 purchase. Internet is needed once to activate; the app works offline afterwards.",
                color = Color(0xFFB9BECA), fontSize = 13.sp
            )
            Spacer(Modifier.height(25.dp))
            OutlinedTextField(
                value = serial, onValueChange = { serial = it.uppercase().take(42) },
                label = { Text("LTI serial number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                enabled = serial.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        busy = true
                        message = ""
                        try {
                            val (token, retailer) = requestActivation(
                                serial, storage.deviceHash, storage.installationId
                            )
                            storage.save(token, retailer)
                            active = true
                        } catch (e: Exception) {
                            message = e.message ?: "Activation failed. Check your connection and code."
                        } finally { busy = false }
                    }
                }
            ) {
                Text(if (busy) "Checking key..." else "Activate this phone")
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = Color(0xFFFFA0A0), fontSize = 12.sp)
            }
            Spacer(Modifier.height(25.dp))
            Text(
                "One active device per licence. Reinstalling needs the serial again. " +
                    "A shared APK is not activated on another phone. 18+ · Unofficial research software.",
                color = Color(0xFF9BA3B0), fontSize = 11.sp
            )
        }
    }
}
