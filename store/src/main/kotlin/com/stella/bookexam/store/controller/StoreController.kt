package com.stella.bookexam.store.controller

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