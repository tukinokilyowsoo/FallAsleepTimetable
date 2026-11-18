package io.github.fallasleeptimetable.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 教务登录信息的本机存储。
 *
 * - 只用 Android Keystore 里的 AES/GCM 密钥加密后写入应用私有 SharedPreferences；
 * - 按站点 host 分别保存，换学校不会串号；
 * - 应用关闭备份，密钥不可导出；
 * - 用户在导入页关闭「记住密码」时清空全部内容。
 */
object EduCredentialStore {
    data class Credentials(val account: String, val password: String)

    private const val PREFS_NAME = "edu_credentials"
    private const val DATA_KEY = "data"
    private const val KEY_ALIAS = "fallasleep_edu_credentials"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    @Synchronized
    private fun readAll(context: Context): MutableMap<String, Credentials> {
        val encoded = prefs(context).getString(DATA_KEY, null) ?: return mutableMapOf()
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = bytes.first().toInt() and 0xFF
            require(ivLength > 0 && bytes.size > ivLength + 1)
            val iv = bytes.copyOfRange(1, 1 + ivLength)
            val body = bytes.copyOfRange(1 + ivLength, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            val json = JSONObject(String(cipher.doFinal(body), Charsets.UTF_8))
            buildMap {
                json.keys().forEach { host ->
                    val item = json.getJSONObject(host)
                    put(
                        host,
                        Credentials(
                            account = item.getString("account"),
                            password = item.getString("password"),
                        ),
                    )
                }
            }.toMutableMap()
        }.getOrElse {
            // 密钥失效或数据损坏：直接丢弃，避免反复抛异常。
            prefs(context).edit().remove(DATA_KEY).apply()
            mutableMapOf()
        }
    }

    @Synchronized
    private fun writeAll(context: Context, all: Map<String, Credentials>): Boolean {
        if (all.isEmpty()) {
            prefs(context).edit().remove(DATA_KEY).apply()
            return true
        }
        return runCatching {
            val json = JSONObject()
            all.forEach { (host, credentials) ->
                json.put(
                    host,
                    JSONObject()
                        .put("account", credentials.account)
                        .put("password", credentials.password),
                )
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val iv = cipher.iv
            val body = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
            val payload = byteArrayOf(iv.size.toByte()) + iv + body
            prefs(context).edit()
                .putString(DATA_KEY, Base64.encodeToString(payload, Base64.NO_WRAP))
                .apply()
            true
        }.getOrDefault(false)
    }

    fun load(context: Context, host: String): Credentials? = readAll(context)[host]

    fun save(context: Context, host: String, credentials: Credentials): Boolean {
        if (host.isBlank() || credentials.account.isBlank() || credentials.password.isEmpty()) {
            return false
        }
        val all = readAll(context)
        all[host] = credentials
        return writeAll(context, all)
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(DATA_KEY).apply()
    }
}
