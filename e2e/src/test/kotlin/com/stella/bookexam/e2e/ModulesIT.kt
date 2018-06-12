package com.stella.bookexam.e2e

import com.stella.bookexam.schema.BookDto
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.concurrent.TimeUnit

class ModulesIT {
    companion object {

        class KDockerComposeContainer(path: File) : DockerComposeContainer<KDockerComposeContainer>(path)


        @ClassRule
        @JvmField
        val env = KDockerComposeContainer(File("../docker-compose.yml"))
                .withLocalCompose(true)

        private var counter = System.currentTimeMillis()

        @BeforeClass
        @JvmStatic
        fun initialize() {
            RestAssured.baseURI = "http://localhost"
            RestAssured.port = 10000
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()


            Awaitility.await().atMost(240, TimeUnit.SECONDS)
                    .ignoreExceptions()
                    .until({
                        RestAssured.given().get("http://localhost:10000/api/v1/user").then().statusCode(401)

                        true
                    })
        }
    }


    class NeededCookies(val session: String, val csrf: String)

    private fun registerUser(id: String, password: String, role: String?): NeededCookies {

        val xsrfToken = RestAssured.given().contentType(ContentType.URLENC)
                .formParam("the_user", id)
                .formParam("the_password", password)
                .formParam("the_role", role?.toUpperCase())
                .post("/api/v1/signIn")
                .then()
                .statusCode(403)
                .extract().cookie("XSRF-TOKEN")

        val session = RestAssured.given().contentType(ContentType.URLENC)
                .formParam("the_user", id)
                .formParam("the_password", password)
                .formParam("the_role", role?.toUpperCase())
                .header("X-XSRF-TOKEN", xsrfToken)
                .cookie("XSRF-TOKEN", xsrfToken)
                .post("/api/v1/signIn")
                .then()
                .statusCode(204)
                .extract().cookie("SESSION")

        return NeededCookies(session, xsrfToken)
    }

    private fun createUniqueId(): String {
        counter++
        return "foo_$counter"
    }


    @Test
    fun testAccessToModulesAndRabbitMq() {

        /***********************************************Region Book API***********************************************/


        /**Performing GET requests to book-server while not authenticated will return 401 status code**/

        RestAssured.given().
                get("/api/v1/book-server/books").
                then().
                statusCode(401)

        /**When authenticated, both USER and ADMIN roles can perform GET requests to book-server**/

        //login as user
        val id = createUniqueId()
        val cookies = registerUser(id, "password", "USER")

        RestAssured.given().
                cookie("SESSION", cookies.session)
                .get("/api/v1/book-server/books")
                .then().
                        statusCode(200)

        /**An user cannot perform POST requests to book-server**/

        val book = BookDto("bookName", "bookDescription", "bookGenre", "bookAuthor", 25, 4)
        val book2 = BookDto("bookName2", "bookDescription2", "bookGenre2", "bookAuthor2", 50, 5)

        //Post
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .body(book)
                .post("/api/v1/book-server/books")
                .then()
                .statusCode(403)

        /**Only admin can perform POST requests to book-server**/

        //Login as admin
        val id2 = createUniqueId()
        val cookies2 = registerUser(id2, "password", "ADMIN")

        //Post
        val bookId = RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .contentType(ContentType.JSON)
                .body(book)
                .post("/api/v1/book-server/books")
                .then()
                .statusCode(201)
                .extract()
                .`as`(Long::class.java)

        /**An user cannot perform PUT or PATCH requests to book-server**/

        //Json merge patch
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", bookId)
                .contentType("application/merge-patch+json")
                .body("{\"name\":null, \"description\":null, \"genre\":\"newGenre\", \"price\":30, \"rating\":2}")
                .patch("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(403)

        //Put
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", bookId)
                .contentType(ContentType.TEXT)
                .body(book2.price.toString())
                .patch("/api/v1/book-server/books/{id}/price")
                .then()
                .statusCode(403)

        /**Admin can perform PUT or PATCH requests to book-server**/

        //Json merge patch
        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .pathParam("id", bookId)
                .contentType("application/merge-patch+json")
                .body("{\"name\":null, \"description\":null, \"genre\":\"newGenre\", \"price\":30, \"rating\":2}")
                .patch("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(204)

        //Put
        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .pathParam("id", bookId)
                .contentType(ContentType.TEXT)
                .body(book2.price.toString())
                .patch("/api/v1/book-server/books/{id}/price")
                .then()
                .statusCode(204)

        /**An user cannot perform DELETE requests to book-server**/

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", bookId)
                .delete("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(403)

        /**Admin can perform DELETE requests to book-server**/

        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", bookId)
                .delete("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(204)

        /***********************************************Region Member API***********************************************/

        /**RabbitMq(when creating user send a message to member module and create a member for that user**/

        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until({
                    RestAssured.given().cookie("SESSION", cookies.session)
                            .get("/api/v1/member-server/members")
                            .then()
                            .statusCode(200)
                            .and()
                            .body("username", CoreMatchers.hasItem(id))
                    true
                })

//                .antMatchers("/v2/api-docs", "/configuration/**", "/swagger-resources/**", "/swagger-ui.html", "/webjars/**", "/api-docs/**").permitAll()
//                .antMatchers(HttpMethod.PATCH, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
//                .antMatchers(HttpMethod.PUT, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
//                .antMatchers(HttpMethod.POST, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
//                .antMatchers(HttpMethod.DELETE, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
//                .antMatchers(HttpMethod.POST, "/members/{id}/books").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
//                .antMatchers(HttpMethod.POST, "/members").hasRole("USER")
//                .antMatchers(HttpMethod.GET, "/members/**").permitAll()
    }
}