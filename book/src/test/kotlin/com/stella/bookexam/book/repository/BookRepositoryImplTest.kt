package com.stella.bookexam.book.repository

import com.stella.bookexam.book.domain.model.Book
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
class BookRepositoryImplTest {
    @Autowired
    private lateinit var repo: BookRepository

    @Before
    fun setup() {
        Assert.assertEquals(0, repo.count())
    }

    /**Valid book**/
    @Test
    fun testCreateBook_Valid() {
        val book = getValidBooks()[0]

        val savedId = repo.createBook(
                book.name,
                book.description!!,
                book.genre!!,
                book.author!!,
                book.price!!,
                book.rating!!
        )

        Assert.assertEquals(1, repo.count())

        val foundBook = repo.findOne(savedId)
        foundBook.name = book.name
    }

    /**Invalid book constraint - price can't be lower than 0**/
    @Test
    fun testCreateBook_priceInvalid() {
        val book = getValidBooks()[0]
        try {
            book.price = -20

            val savedId = repo.createBook(
                    book.name,
                    book.description!!,
                    book.genre!!,
                    book.author!!,
                    book.price!!,
                    book.rating!!

            )
            Assert.fail()

        } catch (e: Exception) {

        }

    }

    /**Invalid book constraint - rating can't be above 5**/
    @Test
    fun testCreateBook_ratingInvalid() {
        val book = getValidBooks()[0]
        try {
            book.rating = 6

            val savedId = repo.createBook(
                    book.name,
                    book.description!!,
                    book.genre!!,
                    book.author!!,
                    book.price!!,
                    book.rating!!

            )
            Assert.fail()

        } catch (e: Exception) {

        }

    }

    /**Get all books**/
    @Test
    fun testGetAll() {
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]
        val book3 = getValidBooks()[2]

        createBook(book1)
        createBook(book2)
        createBook(book3)

        Assert.assertEquals(3, repo.count())

    }

    /**Find all books by a given name**/
    @Test
    fun testFindBookByName() {
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]

        createBook(book1)
        createBook(book2)

        Assert.assertEquals(2, repo.count())
        val bookFound1 = repo.findAllByName(book1.name)

        Assert.assertEquals(1, bookFound1.count())
        Assert.assertTrue(bookFound1.any({ e -> e.name == book1.name }))

    }

    /**Check if a book exist by name**/
    @Test
    fun testExistsByName() {
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]

        Assert.assertEquals(false, repo.existsByName(book1.name))

        createBook(book1)

        Assert.assertEquals(true, repo.existsByName(book1.name))
        Assert.assertEquals(false, repo.existsByName(book2.name))
    }

    fun createBook(book: Book): Long {
        val savedId = repo.createBook(
                book.name,
                book.description,
                book.genre,
                book.author,
                book.price,
                book.rating
        )


        return savedId
    }

    /**Update book**/
    @Test
    fun testUpdateBook_Valid() {
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]

        //create a book
        val savedId = createBook(book1)
        Assert.assertEquals(1, repo.count())

        //find the book
        val foundBook = repo.findOne(savedId)

        book2.id = foundBook.id

        //update
        val wasSuccessful = updateBook(book2, foundBook.id!!)
        Assert.assertEquals(true, wasSuccessful)

        //find the updated book
        val readBook = repo.findOne(foundBook.id!!)

        //assert that the updated book1's name is the same as of book2
        Assert.assertEquals(readBook?.name, book2.name)
        Assert.assertEquals(readBook?.id, foundBook.id)
        Assert.assertEquals(1, repo.count())
    }

    /**Book should not update when invalid id is given**/
    @Test
    fun testUpdateBook_Invalid() {
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]

        val savedId = createBook(book1)

        book2.id = 124124124
        updateBook(book2, book1.id!!)

        // Validate that id did not change
        val bookFound = repo.findOne(124124124)

        Assert.assertNull(bookFound)
        Assert.assertEquals(1, repo.count())
    }

    /**Update book price only**/
    @Test
    fun testUpdatePrice(){
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]

        val savedId = createBook(book1)

        Assert.assertEquals(1,repo.count())

        val foundBook = repo.findOne(savedId)
        book2.id = foundBook.id

        val wasSuccessful = repo.updatePrice(book2.price!!, foundBook.id!!)
        Assert.assertTrue(wasSuccessful)

        val updatedBook = repo.findOne(savedId)

        Assert.assertEquals(book2.price, updatedBook.price)
    }

    /**Delete book**/
    @Test
    fun testDeleteBook() {
        val book1 = getValidBooks()[0]
        val book2 = getValidBooks()[1]

        val savedId1 = createBook(book1)
        val savedId2 = createBook(book2)
        val foundBook1 = repo.findOne(savedId1)
        val foundBook2 = repo.findOne(savedId2)

        Assert.assertEquals(2, repo.count())
        repo.delete(foundBook1.id)

        Assert.assertEquals(1, repo.count())

        repo.delete(foundBook2.id)
        Assert.assertEquals(0, repo.count())
    }


    fun updateBook(book: Book, id: Long): Boolean {
        return repo.updateBook(
                name = book.name,
                description = book.description!!,
                genre = book.genre!!,
                author = book.author!!,
                price = book.price!!,
                rating = book.rating!!,
                id = book.id!!
        )
    }

    fun getValidBooks(): List<Book> {
        return listOf(
                Book(
                        "foo",
                        "fooDesc",
                        "fooGenre",
                        "fooAuthor",
                        10,
                        3,
                        50
                ),
                Book(
                        "bar",
                        "barDesc",
                        "barGenre",
                        "barAuthor",
                        20,
                        4,
                        60
                ),
                Book(
                        "foobar",
                        "foobarDesc",
                        "foobarGenre",
                        "foobarAuthor",
                        40,
                        5,
                        40

                )
        )
    }
}