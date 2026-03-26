package it.pagopa.ecommerce.watchdog.deadletter.config

import com.azure.security.keyvault.certificates.implementation.models.CertificateBundle
import com.azure.security.keyvault.certificates.implementation.models.CertificateListResult
import com.azure.security.keyvault.secrets.implementation.models.SecretAttributes
import com.azure.security.keyvault.secrets.implementation.models.SecretBundle
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.internal.PlatformDependent
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue
import io.netty.util.internal.shaded.org.jctools.queues.unpadded.MpscUnpaddedArrayQueue
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.DeadletterTransactionDto as DeadletterTransactionDtoV2
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ListDeadletterTransactions200ResponseDto as ListDeadletterTransactions200ResponseDtoV2
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NettyNativeConfig {

    /**
     * Registers JCTools and Netty classes for native compilation reflection. This resolves the
     * NoClassDefFoundError for MpscUnpaddedArrayQueue and related classes that Netty uses for
     * buffer allocation in native images.
     *
     * Note: We only register the key classes that are public and accessible. JCTools classes are
     * shaded into Netty, so we use the shaded package paths.
     */
    @Bean
    @RegisterReflectionForBinding(
        // Main Netty buffer allocation classes - these are public and accessible
        PooledByteBufAllocator::class,
        PlatformDependent::class,
        // The specific JCTools class mentioned in the pipeline health check error (shaded path)
        MpscUnpaddedArrayQueue::class,
        MpscArrayQueue::class,
        // auto generated model DTO
        ListDeadletterTransactions200ResponseDto::class,
        AuthenticationCredentialsDto::class,
        AuthenticationOkDto::class,
        DeadletterTransactionDto::class,
        DeadletterTransactionDtoV2::class,
        ListDeadletterTransactions200ResponseDtoV2::class,

        // Azure KeyVault classes
        SecretBundle::class,
        SecretAttributes::class,
        CertificateListResult::class,
        CertificateBundle::class,
    )
    fun nettyNativeConfiguration(): String {
        // Simple bean to trigger the reflection registration
        return "netty-native-config"
    }
}
