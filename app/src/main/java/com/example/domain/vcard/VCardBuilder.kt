package com.example.domain.vcard

import java.nio.charset.StandardCharsets

object VCardBuilder {

    data class ContactData(
        val fullNameLatin: String,
        val profession: String = "",
        val phone: String = "",
        val email: String = "",
        val country: String = "",
        val note: String = ""
    )

    fun buildVCard3(contact: ContactData): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\r\n")
        sb.append("VERSION:3.0\r\n")

        val fn = escape(contact.fullNameLatin)
        sb.append("FN:$fn\r\n")

        val parts = contact.fullNameLatin.split("\\s+".toRegex())
        val lastName = if (parts.size > 1) escape(parts.last()) else ""
        val firstName = if (parts.isNotEmpty()) escape(parts.first()) else ""
        sb.append("N:$lastName;$firstName;;;\r\n")

        if (contact.profession.isNotBlank()) {
            sb.append("TITLE:${escape(contact.profession)}\r\n")
        }
        if (contact.phone.isNotBlank()) {
            sb.append("TEL;TYPE=CELL:${escape(contact.phone)}\r\n")
        }
        if (contact.email.isNotBlank()) {
            sb.append("EMAIL;TYPE=INTERNET:${escape(contact.email)}\r\n")
        }
        if (contact.country.isNotBlank()) {
            sb.append("ADR;TYPE=DOM:;;;${escape(contact.country)};;;\r\n")
        }
        if (contact.note.isNotBlank()) {
            sb.append("NOTE:${escape(contact.note)}\r\n")
        }

        sb.append("END:VCARD\r\n")

        var result = sb.toString()
        // Ensure <= 1000 bytes
        while (result.toByteArray(StandardCharsets.UTF_8).size > 1000 && contact.note.isNotBlank()) {
            // Drop or trim note
            result = result.replace(Regex("NOTE:[^\\r\\n]*\\r\\n"), "")
        }

        return result
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
            .replace("\r", "")
    }
}
