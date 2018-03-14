package com.vertice.teepop.fingerprintapp

import android.Manifest
import android.annotation.SuppressLint

import android.app.KeyguardManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.widget.Toast
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.IOException
import java.security.cert.CertificateException
import javax.crypto.KeyGenerator
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.security.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


@RuntimePermissions
class MainActivity : AppCompatActivity() {

    //Alias for our key in the Android Key Store
    val KEY_NAME: String = "key_name"

    var fingerprintManager: FingerprintManager? = null
    lateinit var keyguardManager: KeyguardManager

    var mKeyStore: KeyStore? = null
    var mKeyGenerator: KeyGenerator? = null
    var cipher: Cipher? = null
    var cryptoObject: FingerprintManager.CryptoObject? = null
    var fingerprintHelper: FingerprintHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager = getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
            fingerprintHelper = FingerprintHelper(this)
        }

    }

    @SuppressLint("InlinedApi", "NewApi")
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPause() {
        super.onPause()
        fingerprintHelper?.stopListening()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }

        try {
            mKeyStore?.load(null)
            val key = mKeyStore?.getKey(KEY_NAME, null) as SecretKey
            cipher?.init(Cipher.ENCRYPT_MODE, key)
            return true

        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKey() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        }

        try {
            mKeyStore?.load(null)
            mKeyGenerator?.init(KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            mKeyGenerator?.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @NeedsPermission(Manifest.permission.USE_FINGERPRINT)
    fun showFingerprintDialog() {
        fingerprintManager?.run {
            if (isHardwareDetected) {
                if (!keyguardManager.isKeyguardSecure) {
                    Toast.makeText(this@MainActivity, "Lock screen security not enabled in Settings", Toast.LENGTH_LONG).show()
                    return
                }
                if (hasEnrolledFingerprints()) {
                    Toast.makeText(this@MainActivity, "Register at least one fingerprint in Settings", Toast.LENGTH_LONG).show()
                    return
                }

                generateKey()
                if (initCipher()) {
                    cryptoObject = FingerprintManager.CryptoObject(cipher)
                    fingerprintHelper?.startAuth(fingerprintManager!!, cryptoObject!!)
                }
            }
        }
    }
}
