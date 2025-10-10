package it.pagopa.ecommerce.watchdog.deadletter.domain

data class UserDetails(val id: String, val name: String, val surname: String, val email: String) {
    fun toMap(): Map<String, String> {
        return mapOf("id" to id, "name" to name, "surname" to surname, "email" to email)
    }
}
