package it.pagopa.ecommerce.watchdog.deadletter

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Calendar
import java.util.Date
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

class JwtKeyGenerationTestUtils {
    companion object {
        @JvmStatic
        fun generatePKCS12Certificate(keyPair: KeyPair): X509Certificate {

            val startDate = Date()
            val endDate = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time

            val certBuilder =
                JcaX509v3CertificateBuilder(
                    X500Name("CN=Test Certificate"),
                    BigInteger.valueOf(System.currentTimeMillis()),
                    startDate,
                    endDate,
                    X500Name("CN=Test Certificate"),
                    keyPair.public,
                )

            val contentSigner = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
            val certHolder: X509CertificateHolder = certBuilder.build(contentSigner)
            val certificate: X509Certificate =
                JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider())
                    .getCertificate(certHolder)

            return certificate
        }

        @JvmStatic
        fun getKeyPairEC(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance("EC")
            keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
            return keyPairGenerator.generateKeyPair()
        }

        @JvmStatic
        fun getKeyPairRSA(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            return keyPairGenerator.generateKeyPair()
        }

        @JvmStatic
        fun getKid(encodedCert: ByteArray): String {
            // Compute SHA-256 hash
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(encodedCert)

            // Convert to Base64 URL-encoded string
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        }

        @JvmStatic
        fun getKeyStoreWithPKCS12Certificate(
            alias: String,
            keyPair: KeyPair,
            password: String,
        ): KeyStore {
            val certificate = this.generatePKCS12Certificate(keyPair)

            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null, null)
            keyStore.setKeyEntry(
                alias,
                keyPair.private,
                password.toCharArray(),
                arrayOf(certificate),
            )

            return keyStore
        }

        @JvmStatic
        fun generatePKCS12CertificateAsBase64(keyStore: KeyStore, password: String): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            keyStore.store(byteArrayOutputStream, password.toCharArray())
            val keyStoreBytes = byteArrayOutputStream.toByteArray()

            return Base64.getEncoder().encodeToString(keyStoreBytes)
        }
    }
}
