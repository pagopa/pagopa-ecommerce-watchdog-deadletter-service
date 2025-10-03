package it.pagopa.ecommerce.watchdog.deadletter.config

import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.internal.PlatformDependent
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue
import io.netty.util.internal.shaded.org.jctools.queues.unpadded.MpscUnpaddedArrayQueue
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
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
        // auto generated DTO
        ListDeadletterTransactions200ResponseDto::class,
        AuthenticationCredentialsDto::class,
        AuthenticationOkDto::class,
        DeadletterTransactionDto::class
    )
    fun nettyNativeConfiguration(): String {
        // Simple bean to trigger the reflection registration
        return "netty-native-config"
    }
}