package com.stella.bookexam.store.controller

import com.stella.bookexam.schema.BookForSaleDto
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test

class StoreControllerTest : TestBase(){
    @Test
    fun testCleanDB() {
        RestAssured.given()
                .get().then()
                .statusCode(200)
                .body("size()", CoreMatchers.equalTo(0))
    }

    /**Create a book for sale**/
    @Test
    fun testCreateBookForSale() {
        var dto = getValidBookForSaleDto()

        // valid dto
        val id = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(201)
                .extract().`as`(Long::class.java)
        Assert.assertNotNull(id)

        // invalid dto
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("wrongBody")
                .post()
                .then()
                .statusCode(400)

    }

    /**Get a book for sale**/
    @Test
    fun testGetBookForSale() {
        val dto = getValidBookForSaleDto()
        val id = postBookForSaleValid(dto)

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get().then().statusCode(200)
                .body("size()", CoreMatchers.equalTo(1))

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .param("name", dto.name)
                .get().then().statusCode(200)
                .body("size()", CoreMatchers.equalTo(1))

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .param("name", "wrongUsername")
                .get().then()
                .statusCode(200).body("size()", CoreMatchers.equalTo(0))
    }

    /**Get a book for sale by id**/
    @Test
    fun testGetBookForSaleById() {
        val dto = getValidBookForSaleDto()
        val id = postBookForSaleValid(dto)


        //Get a valid book for sale
        val response1 = RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", id)
                .get("/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(BookForSaleDto::class.java)
        Assert.assertTrue(dto.name == response1.name)

        //Invalid id param
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", "invalid_input")
                .get("/{id}")
                .then()
                .statusCode(400)

        //Non existing id

        val nonExistId = 555
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", nonExistId)
                .get("{id}")
                .then()
                .statusCode(404)
    }

    /**Delete a book for sale by id**/
    @Test
    fun testDeleteBookForSale() {
        val dto = getValidBookForSaleDto()
        val id = postBookForSaleValid(dto)


        //Valid
        RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().basic("admin", "admin")
                .accept(ContentType.JSON)
                .pathParam("id", id)
                .delete("/{id}")
                .then()
                .statusCode(204)

        //Invalid id input
        RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().basic("admin", "admin")
                .accept(ContentType.JSON)
                .pathParam("id", "invalid_input")
                .delete("/{id}")
                .then()
                .statusCode(400)

        //Non existing id
        val nonExistId = 555
        RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().basic("admin", "admin")
                .accept(ContentType.JSON)
                .pathParam("id", nonExistId)
                .delete("{id}")
                .then()
                .statusCode(404)
    }


    fun getValidBookForSaleDto():BookForSaleDto {

        return BookForSaleDto("bookName", "bookOwner", 5)
    }

    fun postBookForSaleValid(dto: BookForSaleDto) : Long{
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(201)
                .extract().`as`(Long::class.java)
    }
}