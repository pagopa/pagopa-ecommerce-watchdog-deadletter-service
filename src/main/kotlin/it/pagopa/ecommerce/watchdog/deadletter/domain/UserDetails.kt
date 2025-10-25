package it.pagopa.ecommerce.watchdog.deadletter.domain

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserDetails(val id: String, val name: String, val surname: String, val email: String) :
    UserDetails {

    fun toMap(): Map<String, String> =
        mapOf("id" to id, "name" to name, "surname" to surname, "email" to email)

    override fun getAuthorities(): Collection<GrantedAuthority?> = emptyList()

    override fun getPassword(): String? = null

    override fun getUsername(): String? = null
}
