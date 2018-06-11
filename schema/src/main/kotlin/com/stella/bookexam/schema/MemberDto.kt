package com.stella.bookexam.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable
import java.time.ZonedDateTime

@ApiModel("DTO representing Member")
data class MemberDto(

        @ApiModelProperty("Username of the member")
        var username: String? = null,

        @ApiModelProperty("Books that the member offers for sale")
        var books: MutableMap<String, Int>? = null,

        @ApiModelProperty("The id of the member")
        var id: String? = null,

        @ApiModelProperty("When the member was created")
        var memberSince: ZonedDateTime? = null

): Serializable