package com.stella.bookexam.gateway.domain.dto

import java.io.Serializable

class UserDto(
        var username: String?=null,
        var password: String?=null,
        var roles: Set<String>? = null,
        var enabled: Boolean? = null
): Serializable