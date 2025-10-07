package it.pagopa.ecommerce.watchdog.deadletter.services.jwt

import com.azure.security.keyvault.certificates.CertificateAsyncClient
import com.azure.security.keyvault.certificates.models.KeyVaultCertificate
import com.azure.security.keyvault.secrets.SecretAsyncClient
import com.azure.security.keyvault.secrets.models.KeyVaultSecret
import it.pagopa.ecommerce.watchdog.deadletter.config.jwt.AzureSecretConfigProperties
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PublicKeyWithKid
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.OffsetDateTime
import java.util.Base64

@Component
class ReactiveAzureKVSecurityKeysService(
    private val secretClient: SecretAsyncClient,
    private val certClient: CertificateAsyncClient,
    private val azureSecretConfig: AzureSecretConfigProperties,
) : IReactiveSecurityKeysService {
    private val keystore = KeyStore.getInstance("PKCS12")
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
            val decodedPfx = Base64.getDecoder().decode(it.value)
            keystore.load(
                ByteArrayInputStream(decodedPfx),
                azureSecretConfig.password.toCharArray(),
            )
            keystore
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
}