package com.stella.bookexam.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for books that are set for sale")
data class BookForSaleDto(

        @ApiModelProperty("The name of the book")
        var name: String?=null,

        @ApiModelProperty("The seller of the book")
        var soldBy: String?=null,

        @ApiModelProperty("Price that the book is selling for")
        var price: Int?=null

) : Serializable