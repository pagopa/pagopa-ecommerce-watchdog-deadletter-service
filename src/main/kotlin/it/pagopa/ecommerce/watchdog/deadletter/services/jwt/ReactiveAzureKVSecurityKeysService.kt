package it.pagopa.ecommerce.watchdog.deadletter.services.jwt

import com.azure.security.keyvault.certificates.CertificateAsyncClient
import com.azure.security.keyvault.certificates.models.KeyVaultCertificate
import com.azure.security.keyvault.secrets.SecretAsyncClient
import com.azure.security.keyvault.secrets.models.KeyVaultSecret
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyOperation
import com.nimbusds.jose.jwk.KeyUse
import it.pagopa.ecommerce.watchdog.deadletter.config.azure.AzureSecretConfigProperties
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PublicKeyWithKid
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.OffsetDateTime
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class ReactiveAzureKVSecurityKeysService(
    private val secretClient: SecretAsyncClient,
    private val certClient: CertificateAsyncClient,
    private val azureSecretConfig: AzureSecretConfigProperties,
) : IReactiveSecurityKeysService {
    private val certFactory = CertificateFactory.getInstance("X.509")
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getSecret(): Mono<KeyVaultSecret> {
        return secretClient.getSecret(azureSecretConfig.name)
    }

    fun getCerts(): Flux<KeyVaultCertificate> {
        return certClient
            .listPropertiesOfCertificateVersions(azureSecretConfig.name)
            .doOnNext {
                logger.debug(
                    "CertificateProperties - name: {}, version: {}, enabled: {}, expiresOn: {}, notBefore: {}, createdOn: {}, updatedOn: {}",
                    it.name,
                    it.version,
                    it.isEnabled,
                    it.expiresOn,
                    it.notBefore,
                    it.createdOn,
                    it.updatedOn,
                )
            }
            .filter {
                it.isEnabled && (it.expiresOn == null || it.expiresOn.isAfter(OffsetDateTime.now()))
            }
            .flatMap {
                certClient
                    .getCertificateVersion(azureSecretConfig.name, it.version)
                    .onErrorResume { exception ->
                        logger.error("Failed to retrieve certificate version", exception)
                        Mono.empty()
                    }
            }
    }

    fun getKeyStore(): Mono<KeyStore> {
        return this.getSecret().map {
            val keystore = KeyStore.getInstance("PKCS12")
            val decodedPfx = Base64.getDecoder().decode(it.value)
            keystore.load(
                ByteArrayInputStream(decodedPfx),
                azureSecretConfig.password.toCharArray(),
            )
            keystore
        }
    }

    /*
    fun getPublicJwkFromKeyStore(): Mono<JWK> {
        return this.getKeyStore().map {
            val alias = it.aliases().nextElement()
            val certificate = it.getCertificate(alias) as X509Certificate
            val kid = getKid(certificate.encoded)
            val publicKey = certificate.publicKey as ECPublicKey

            ECKey.Builder(Curve.P_256, publicKey)
                .keyID(kid)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .build()
        }
    }*/
    fun getPublicJwkFromKeyStore(): Mono<JWK> {
        return this.getKeyStore().map {
            val alias = it.aliases().nextElement()
            val certificate = it.getCertificate(alias) as X509Certificate
            val kid = getKid(certificate.encoded)
            val publicKey = certificate.publicKey as ECPublicKey

            logger.info("Public JWK loaded from PFX alias=$alias kid=$kid")
            ECKey.Builder(Curve.P_256, publicKey)
                .keyID(kid)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .build()
        }
    }

    fun getSignerJwk(): Mono<ECKey> {
        return this.getKeyStore().map {
            val alias = it.aliases().nextElement()
            val certificate = it.getCertificate(alias) as X509Certificate
            val kid = getKid(certificate.encoded)
            val publicKey = certificate.publicKey as ECPublicKey
            val privateKey =
                it.getKey(alias, azureSecretConfig.password.toCharArray()) as ECPrivateKey

            ECKey.Builder(Curve.P_256, publicKey)
                .privateKey(privateKey)
                .keyID(kid)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .keyOperations(setOf(KeyOperation.SIGN, KeyOperation.VERIFY))
                .build()
        }
    }

    override fun getPrivate(): Mono<PrivateKeyWithKid> {
        return this.getKeyStore().map {
            val alias = it.aliases().nextElement()
            PrivateKeyWithKid(
                getKid(it.getCertificate(alias).encoded),
                it.getKey(alias, azureSecretConfig.password.toCharArray()) as PrivateKey,
            )
        }
    }

    override fun getPublic(): Flux<PublicKeyWithKid> {
        return this.getCerts().map {
            val x509Cert: X509Certificate =
                certFactory.generateCertificate(ByteArrayInputStream(it.cer)) as X509Certificate
            PublicKeyWithKid(getKid(it.cer), x509Cert.publicKey)
        }
    }

    private fun getKid(encodedCert: ByteArray): String {
        // Compute SHA-256 hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(encodedCert)
        // Convert to Base64 URL-encoded string
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    /*
    private val mockKeyPair: KeyPair by lazy {
        logger.warn("!!! USING MOCKED KEYPAIR FOR LOCAL TESTING !!!")
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(ecSpec)
        keyPairGenerator.genKeyPair()
    }

    override fun getPrivate(): Mono<PrivateKeyWithKid> =
        Mono.just(PrivateKeyWithKid(kid = "mock-kid-private", privateKey = mockKeyPair.private))

    override fun getPublic(): Flux<PublicKeyWithKid> =
        Flux.just(PublicKeyWithKid(kid = "mock-kid-public", publicKey = mockKeyPair.public))
     */
}
