package com.stella.bookexam.book.controller

import com.stella.bookexam.book.BookApplication
import com.stella.bookexam.book.repository.BookRepository
import io.restassured.RestAssured
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = arrayOf(BookApplication::class),
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class TestBase {

    @LocalServerPort
    protected var port = 0

    @Autowired
    private lateinit var bookRepository: BookRepository

    @Before
    @After
    fun clean() {

        // RestAssured configs shared by all the tests
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        RestAssured.basePath = "/books"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()


        bookRepository.deleteAll()

    }
}