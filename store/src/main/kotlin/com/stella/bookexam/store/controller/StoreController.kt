package com.stella.bookexam.store.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.stella.bookexam.schema.BookForSaleDto
import com.stella.bookexam.store.domain.converter.BookForSaleConverter
import com.stella.bookexam.store.repository.StoreRepository
import io.swagger.annotations.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.ConstraintViolationException

@Api(value = "/store", description = "API for book store.")
@RequestMapping(
        path = arrayOf("/store"),
        produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
)
@RestController
@Validated
class StoreController {
    @Autowired
    private lateinit var repo: StoreRepository

    @RabbitListener(queues = arrayOf("#{queue.name}"))
    fun createBookForSaleRabbit(bookForSaleDto: BookForSaleDto) {
        registerBookForSale(bookForSaleDto)
    }

    @ApiOperation("Create a book for sale")
    @PostMapping(consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
            ApiResponse(code = 201, message = "Book for sale created, return id of new resource"),
            ApiResponse(code = 409, message = "Dto properties does not follow constraints"),
            ApiResponse(code = 400, message = "Dto does not have required properties")
    )
    fun createBookForSale(
            @ApiParam("Book for sale")
            @RequestBody resultDto: BookForSaleDto
    ): ResponseEntity<Long> {

        if (!validDto(resultDto)) {
            return ResponseEntity.status(400).build()
        }

        try {
            val id = registerBookForSale(resultDto)
            return ResponseEntity.status(201).body(id)
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(409).build()
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }

    }

    @ApiOperation("Fetch all books for sale by default or with specific name if provided in request parameters")
    @ApiResponse(code = 200, message = "List of books for sale")
    @GetMapping
    fun getBooksForSale(
            @ApiParam("The specific name as parameter")
            @RequestParam("name", required = false)
            name: String?
    ): ResponseEntity<List<BookForSaleDto>> {

        when (name.isNullOrBlank()) {
            true ->
                return ResponseEntity.ok(BookForSaleConverter.transform(repo.findAll()) as List<BookForSaleDto>)
            false ->
                return ResponseEntity.ok(BookForSaleConverter.transform(repo.findAllByName(name!!)) as List<BookForSaleDto>)
        }
    }

    @ApiOperation("Get a single book for sale specified by id")
    @ApiResponses(
            ApiResponse(code = 400, message = "Given path param is invalid, can not be parsed to long"),
            ApiResponse(code = 404, message = "Book for sale with given id not found"),
            ApiResponse(code = 200, message = "Return book for sale with given id")
    )
    @GetMapping(path = arrayOf("/{id}"))
    fun getBookForSaleById(
            @ApiParam("Id")
            @PathVariable("id") pathId: Long
    ): ResponseEntity<BookForSaleDto> {
        val dto = repo.findOne(pathId) ?: return ResponseEntity.status(404).build()
        return ResponseEntity.ok(BookForSaleConverter.transform(dto))
    }

    @ApiOperation("Delete a book for sale entity with the given id")
    @ApiResponses(
            ApiResponse(code = 400, message = "Given path param is invalid, can not be parsed to long"),
            ApiResponse(code = 404, message = "Book for sale with given id not found"),
            ApiResponse(code = 204, message = "Book for sale with given id was deleted")
    )
    @DeleteMapping(path = arrayOf("/{id}"))
    fun deleteBookForSale(
            @ApiParam("Id")
            @PathVariable("id")
            pathId: Long
    ): ResponseEntity<Any> {

        if (!repo.exists(pathId)) {
            return ResponseEntity.status(404).build()
        }
        repo.delete(pathId)
        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Modify the book for sale using JSON Merge Patch")
    @PatchMapping(path = arrayOf("/{id}"),
            consumes = arrayOf("application/merge-patch+json"))
    fun mergePatchMember(@ApiParam("Id of the book for sale")
                         @PathVariable("id")
                         id: String?,
                         @ApiParam("The partial patch")
                         @RequestBody
                         jsonPatch: String)
            : ResponseEntity<Void> {

        val pathId : Long
        try{
            pathId = id!!.toLong()
        } catch (e: Exception){
            return ResponseEntity.status(400).build()
        }
        val dto = repo.findOne(pathId) ?: return ResponseEntity.status(404).build()

        val jackson = ObjectMapper()

        val jsonNode: JsonNode
        try {
            jsonNode = jackson.readValue(jsonPatch, JsonNode::class.java)
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }

        if (jsonNode.has("id")) {
            return ResponseEntity.status(409).build()
        }

        var newName = dto.name
        var newSoldBy = dto.soldBy
        var newPrice = dto.price

        //in this case all fields are mandatory, if set to null we will keep the old value
        if (jsonNode.has("name")) {
            val nameNode = jsonNode.get("name")
            if (nameNode.isNull) {
                newName = dto.name
            } else if (nameNode.isTextual) {
                newName = nameNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("soldBy")) {
            val soldByNode = jsonNode.get("soldBy")
            if (soldByNode.isNull) {
                newSoldBy = dto.soldBy
            } else if (soldByNode.isTextual) {
                newSoldBy = soldByNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("price")) {
            val priceNode = jsonNode.get("price")
            if (priceNode == null) {
                newPrice = dto.price
            } else if (priceNode.isNumber) {
                newPrice = priceNode.asInt()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        //now that the input is validated, do the update
        dto.name = newName
        dto.soldBy = newSoldBy
        dto.price = newPrice

        try {
            val successful = repo.update(
                    dto.name,
                    dto.soldBy,
                    dto.price,
                    dto.id!!.toLong()
            )
            if (!successful) {
                return ResponseEntity.status(400).build()
            }
            return ResponseEntity.status(204).build()
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(400).build()
        }

    }

    fun registerBookForSale(bookForSaleDto: BookForSaleDto): Long {
        return repo.createBookForSale(
                bookForSaleDto.name!!,
                bookForSaleDto.soldBy!!,
                bookForSaleDto.price!!
        )

    }


    fun validDto(bookForSaleDto: BookForSaleDto): Boolean {

        if (
                bookForSaleDto.name != null &&
                bookForSaleDto.soldBy != null &&
                bookForSaleDto.price != null &&
                bookForSaleDto.id == null

        ) {
            return true
        }

        return false
    }
}