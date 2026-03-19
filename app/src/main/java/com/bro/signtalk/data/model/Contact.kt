package com.bro.signtalk.data.model

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val profileUri: String? = null
)