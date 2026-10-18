package it.pagopa.ecommerce.watchdog.deadletter.services

import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class TotpSecretsProviderTest {

    @Test
    fun `returns the operator secret from the JSON`() {
        val provider = TotpSecretsProvider("""{"op":"JBSWY3DPEHPK3PXP"}""")

        StepVerifier.create(provider.getSecret("op"))
            .expectNext("JBSWY3DPEHPK3PXP")
            .verifyComplete()
        StepVerifier.create(provider.getSecret("missing")).verifyComplete()
    }

    @Test
    fun `returns empty for a malformed JSON`() {
        StepVerifier.create(TotpSecretsProvider("not json").getSecret("op")).verifyComplete()
    }
}
