package it.pagopa.ecommerce.watchdog.deadletter.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserDetailsTest {

    private val userDetails =
        UserDetails(
            id = "test-id",
            name = "Mario",
            surname = "Rossi",
            email = "mario.rossi@test.it",
        )

    @Test
    fun `toMap should return correct map of user properties`() {
        val userMap = userDetails.toMap()

        val expectedMap =
            mapOf(
                "id" to "test-id",
                "name" to "Mario",
                "surname" to "Rossi",
                "email" to "mario.rossi@test.it",
            )

        assertEquals(expectedMap, userMap)
        assertEquals(4, userMap.size)
    }

    @Test
    fun `getAuthorities should return empty list`() {
        val authorities = userDetails.getAuthorities()

        assertTrue(authorities.isEmpty())
    }

    @Test
    fun `getPassword should return null`() {
        assertNull(userDetails.getPassword())
    }

    @Test
    fun `getUsername should return null`() {
        assertNull(userDetails.getUsername())
    }
}
