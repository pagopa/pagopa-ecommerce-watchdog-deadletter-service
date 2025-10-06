package it.pagopa.ecommerce.watchdog.deadletter.utils

object ObfuscationUtils {
    /**
     * Replaces an email address with "***". If the input is null, it returns null.
     *
     * NOTE: This is a basic placeholder for the obfuscation logic and should be reviewed.
     *
     * @param email the email to obfuscate
     * @return the string "***" or null
     */
    fun obfuscateEmail(email: String?): String? {
        return if (email != null) "***" else null
    }
}
