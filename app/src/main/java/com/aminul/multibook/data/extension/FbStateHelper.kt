package com.aminul.multibook.data.extension

import android.webkit.CookieManager
import androidx.webkit.ProfileStore
import com.aminul.multibook.data.profile.ProfileManager
import com.aminul.multibook.data.util.DiagnosticLogger
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object FbStateHelper {
    private const val TAG = "FbStateHelper"
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    data class CookieItem(
        val key: String,
        val value: String,
        val domain: String = "facebook.com",
        val path: String = "/",
        val hostOnly: Boolean = false,
        val creation: String = isoFormat.format(Date()),
        val lastAccessed: String = isoFormat.format(Date())
    )

    private fun getCookieManager(accountId: String): CookieManager {
        val profileName = "${ProfileManager.PROFILE_PREFIX}$accountId"
        return if (ProfileManager.isMultiProfileSupported()) {
            try {
                ProfileStore.getInstance().getProfile(profileName)?.cookieManager
                    ?: CookieManager.getInstance()
            } catch (_: Exception) {
                CookieManager.getInstance()
            }
        } else {
            CookieManager.getInstance()
        }
    }

    /**
     * Extracts all cookies for the specified account and returns a standard c3c-fbstate JSON string.
     */
    fun exportFbState(accountId: String): String {
        val cm = getCookieManager(accountId)
        val now = isoFormat.format(Date())

        val cookiesStr = (cm.getCookie("https://facebook.com") ?: "") + "; " +
                (cm.getCookie("https://m.facebook.com") ?: "") + "; " +
                (cm.getCookie("https://www.facebook.com") ?: "")

        val cookieMap = mutableMapOf<String, String>()
        cookiesStr.split(";").forEach { pair ->
            val trimmed = pair.trim()
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val k = trimmed.substring(0, eqIdx).trim()
                val v = trimmed.substring(eqIdx + 1).trim()
                if (k.isNotEmpty() && v.isNotEmpty()) {
                    cookieMap[k] = v
                }
            }
        }

        val jsonArray = JSONArray()
        cookieMap.forEach { (key, value) ->
            val obj = JSONObject().apply {
                put("key", key)
                put("value", value)
                put("domain", "facebook.com")
                put("path", "/")
                put("hostOnly", false)
                put("creation", now)
                put("lastAccessed", now)
            }
            jsonArray.put(obj)
        }

        DiagnosticLogger.info(TAG, "Exported FBState for account $accountId (${cookieMap.size} cookies)")
        return jsonArray.toString(4)
    }

    /**
     * Imports standard c3c-fbstate JSON into the specified account's isolated profile.
     */
    fun importFbState(
        accountId: String,
        jsonString: String,
        onComplete: (success: Boolean, count: Int, error: String?) -> Unit
    ) {
        try {
            val cm = getCookieManager(accountId)
            val jsonArray = JSONArray(jsonString.trim())
            var count = 0

            // Set cookies for both https://facebook.com and https://m.facebook.com
            val expirationSeconds = (System.currentTimeMillis() / 1000) + (86400 * 90) // 90 days

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val key = obj.optString("key")
                val value = obj.optString("value")
                val path = if (obj.has("path")) obj.optString("path", "/") else "/"

                if (key.isNotEmpty() && value.isNotEmpty()) {
                    val cookieStr = "$key=$value; domain=.facebook.com; path=$path; Max-Age=7776000; Secure; SameSite=Lax"
                    cm.setCookie("https://facebook.com", cookieStr)
                    cm.setCookie("https://m.facebook.com", cookieStr)
                    cm.setCookie("https://www.facebook.com", cookieStr)
                    count++
                }
            }
            cm.flush()
            DiagnosticLogger.info(TAG, "Imported $count FBState cookies for account $accountId")
            onComplete(true, count, null)
        } catch (e: Exception) {
            DiagnosticLogger.error(TAG, "Failed to import FBState: ${e.message}")
            onComplete(false, 0, e.message ?: "Invalid FBState JSON")
        }
    }

    /**
     * Safe logout that keeps 'sb' and 'dbln' so the account isn't completely forgotten by device.
     */
    fun safeLogout(accountId: String, onComplete: () -> Unit) {
        val cm = getCookieManager(accountId)
        val cookiesStr = cm.getCookie("https://facebook.com") ?: ""

        val keepKeys = setOf("sb", "dbln")
        val allCookies = cookiesStr.split(";").mapNotNull {
            val pair = it.trim().split("=")
            if (pair.size >= 2) pair[0].trim() to pair.subList(1, pair.size).joinToString("=").trim() else null
        }

        allCookies.forEach { (name, _) ->
            if (name !in keepKeys) {
                cm.setCookie("https://facebook.com", "$name=; domain=.facebook.com; path=/; Max-Age=0")
                cm.setCookie("https://m.facebook.com", "$name=; domain=.facebook.com; path=/; Max-Age=0")
                cm.setCookie("https://www.facebook.com", "$name=; domain=.facebook.com; path=/; Max-Age=0")
            }
        }
        cm.flush()
        DiagnosticLogger.info(TAG, "Safe logout executed for account $accountId")
        onComplete()
    }

    /**
     * AES-CTR encryption matching c3c-fbstate aesjs implementation.
     */
    fun encryptFbState(plainJson: String, password: String): String {
        val keyBytes = sha256Bytes(password)
        val iv = ByteArray(16)
        iv[15] = 1 // default aesjs counter = 1

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        return bytesToHex(encrypted)
    }

    /**
     * AES-CTR decryption matching c3c-fbstate aesjs implementation.
     */
    fun decryptFbState(encryptedHex: String, password: String): String {
        val keyBytes = sha256Bytes(password)
        val iv = ByteArray(16)
        iv[15] = 1

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        val decrypted = cipher.doFinal(hexToBytes(encryptedHex.trim()))
        return String(decrypted, Charsets.UTF_8)
    }

    private fun sha256Bytes(input: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = java.lang.StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
