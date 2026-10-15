package com.example.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "City7z7MasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    init {
        getOrCreateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts in-memory data (e.g. for private notes).
     * Returns Pair(cipherBytes, ivBytes)
     */
    fun encryptBytes(plainBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainBytes)
        return Pair(cipherBytes, iv)
    }

    /**
     * Decrypts in-memory cipher with iv.
     */
    fun decryptBytes(cipherBytes: ByteArray, iv: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(cipherBytes)
    }

    /**
     * Encrypts an input stream to a destination file.
     * Prepends 12-byte IV at file start.
     */
    fun encryptStreamToFile(inputStream: InputStream, destinationFile: File) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { fos ->
            fos.write(iv) // 12 bytes IV
            CipherOutputStream(fos, cipher).use { cos ->
                inputStream.copyTo(cos)
            }
        }
    }

    /**
     * Decrypts an encrypted file to an output stream.
     * Reads the 12-byte IV from file start.
     */
    fun decryptFileToStream(sourceFile: File, outputStream: OutputStream) {
        val key = getOrCreateKey()
        FileInputStream(sourceFile).use { fis ->
            val iv = ByteArray(GCM_IV_LENGTH)
            val bytesRead = fis.read(iv)
            if (bytesRead != GCM_IV_LENGTH) {
                throw IllegalStateException("Invalid encrypted file header (IV length mismatch)")
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            CipherInputStream(fis, cipher).use { cis ->
                cis.copyTo(outputStream)
            }
        }
    }

    /**
     * Decrypts an encrypted file completely into memory.
     */
    fun decryptFileToBytes(sourceFile: File): ByteArray {
        val key = getOrCreateKey()
        return FileInputStream(sourceFile).use { fis ->
            val iv = ByteArray(GCM_IV_LENGTH)
            val bytesRead = fis.read(iv)
            if (bytesRead != GCM_IV_LENGTH) {
                throw IllegalStateException("Invalid encrypted file header (IV length mismatch)")
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val cipherBytes = fis.readBytes()
            cipher.doFinal(cipherBytes)
        }
    }
}
