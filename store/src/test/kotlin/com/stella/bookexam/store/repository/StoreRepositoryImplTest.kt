package com.stella.bookexam.store.repository

import com.stella.bookexam.store.domain.model.BookForSale
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
class StoreRepositoryImplTest {
    @Autowired
    private lateinit var crud: StoreRepository

    @Before
    fun setup(){
        Assert.assertEquals(0, crud.count())
    }

    /**Create a valid book for sale**/
    @Test
    fun testCreateBookForSale_Valid() {

        val id = createBookForSaleValid()
        Assert.assertNotNull(id)
        Assert.assertTrue(id != (-1L))
    }

    /**Get all books for sale**/
    @Test
    fun testCreate3BooksForSale_Valid() {

        val id1 = createBookForSaleValid()
        val id2 = createBookForSaleValid()
        val id3 = createBookForSaleValid()


        Assert.assertNotNull(id1)
        Assert.assertNotNull(id2)
        Assert.assertNotNull(id3)
        Assert.assertEquals(3, crud.count())
    }

    /**Get all books for sale filtered by name**/
    @Test
    fun testGetBooksForSaleByName(){

        val name = "bookName"
        createBookForSaleValid()
        createBookForSaleValid()
        createBookForSaleValid()

        val result : List<BookForSale> = crud.findAllByName(name) as List<BookForSale>

        Assert.assertEquals(3, result.size)

    }

    /**Get all books for sale filtered by price**/
    @Test
    fun testGetBooksForSaleByPrice(){

        createBookForSaleValid()
        createBookForSaleValid()
        crud.createBookForSale("newBook", "newOwner", 40)

        val result : List<BookForSale> = crud.findAllByPrice(40) as List<BookForSale>

        Assert.assertEquals(1, result.size)

    }

    /**Get all books for sale filtered by sold by**/
    @Test
    fun testGetBooksForSaleBySoldBy(){

        createBookForSaleValid()
        createBookForSaleValid()
        crud.createBookForSale("newBook", "newOwner", 40)

        val result : List<BookForSale> = crud.findAllBySoldBy("newOwner") as List<BookForSale>

        Assert.assertEquals(1, result.size)

    }

    private fun createBookForSaleValid() : Long {
        return crud.createBookForSale(
                "bookName",
                "bookOwner",
                15)
    }

}