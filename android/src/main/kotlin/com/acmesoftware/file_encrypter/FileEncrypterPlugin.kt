package com.acmesoftware.file_encrypter

import android.util.Base64
import io.flutter.embedding.engine.plugins.FlutterPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec


/** FileEncrypterPlugin */
class FileEncrypterPlugin : FlutterPlugin, FileEncrypterApi {
    private val algorithm = "AES"
    private val transformation = "AES/CBC/PKCS5Padding"
    private val bufferSize = 131072

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        FileEncrypterApi.setUp(binding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        FileEncrypterApi.setUp(binding.binaryMessenger, null)
    }

    override fun encrypt(
        inFileName: String, outFileName: String, callback: (Result<String>) -> Unit
    ) {
        CoroutineScope(IO).launch {
            // val cipher = Cipher.getInstance(transformation)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            // val secretKey = KeyGenerator.getInstance("AES").generateKey()
            val secretKey = SecretKeySpec("Your16ByteAESKey".toByteArray(), "AES")

            try {
                encryptFile(inFileName, outFileName, cipher, secretKey)
                val cipheredText = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
                callback(Result.success(cipheredText))
            } catch (e: Exception) {
                e.printStackTrace()
                callback(Result.failure(FileEncrypterError("ENCRYPTION_FAILED", e.message)))
            }
        }
    }

    override fun decrypt(
        key: String, inFileName: String, outFileName: String, callback: (Result<Unit>) -> Unit
    ) {
        CoroutineScope(IO).launch {
            // val cipher = Cipher.getInstance(transformation)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            val encodedKey = Base64.decode(key, Base64.DEFAULT)
            // val secretKey = SecretKeySpec(encodedKey, 0, encodedKey.size, algorithm)
            // val secretKey = SecretKeySpec(encodedKey, "AES")
            val secretKey = SecretKeySpec("Your16ByteAESKey".toByteArray(), "AES")

            try {
                decryptFile(inFileName, outFileName, cipher, secretKey)
                callback(Result.success(Unit))
            } catch (e: Exception) {
                e.printStackTrace()
                callback(Result.failure(FileEncrypterError("ENCRYPTION_FAILED", e.message)))
            }
        }
    }

    private fun encryptFile(
        inFileName: String,
        outFileName: String,
        cipher: Cipher,
        secretKey: javax.crypto.SecretKey
    ) {
        val iv = "Your16ByteIVHere".toByteArray()

        FileOutputStream(outFileName).use { fileOut ->
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            // val iv = cipher.iv
            // fileOut.write(iv)
            // println("Encrypting: IV value=$iv")

            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                val buffer = ByteArray(bufferSize)
                FileInputStream(inFileName).use { fileIn ->
                    var byteCount = fileIn.read(buffer)
                    while (byteCount != -1) {
                        cipherOut.write(buffer, 0, byteCount)
                        byteCount = fileIn.read(buffer)
                    }
                }
            }
        }
    }

    private fun decryptFile(
        inFileName: String,
        outFileName: String,
        cipher: Cipher,
        secretKey: javax.crypto.SecretKey
    ) {
        val iv = "Your16ByteIVHere".toByteArray()

        FileInputStream(inFileName).use { fileIn ->
            // val fileIv = ByteArray(10)
            // fileIn.read(fileIv)
            // println("Decrypting: IV value=$fileIv")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            val buffer = ByteArray(bufferSize)
            CipherInputStream(fileIn, cipher).use { cipherIn ->
                FileOutputStream(outFileName).use { fileOut ->
                    var byteCount = cipherIn.read(buffer)
                    while (byteCount != -1) {
                        fileOut.write(buffer, 0, byteCount)
                        byteCount = cipherIn.read(buffer)
                    }
                }
            }
        }
    }
}
