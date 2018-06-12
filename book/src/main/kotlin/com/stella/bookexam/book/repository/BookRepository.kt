package com.stella.bookexam.book.repository

import com.stella.bookexam.book.domain.model.Book
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
interface BookRepository : CrudRepository<Book, Long>, BookRepositoryCustom {

    fun findAllByName(name: String): Iterable<Book>

    fun existsByName(name: String): Boolean
}

@Transactional
interface BookRepositoryCustom {
    fun createBook(
            name: String,
            description: String?,
            genre: String?,
            author: String?,
            price: Int?,
            rating: Int?
    ): Long

    fun updateBook(
            name: String,
            description: String?,
            genre: String?,
            author: String?,
            price: Int?,
            rating: Int?,
            id: Long): Boolean

    fun updatePrice(price: Int, id: Long): Boolean

}

open class BookRepositoryImpl : BookRepositoryCustom {


    @PersistenceContext
    private lateinit var em: EntityManager

    override fun createBook(
            name: String,
            description: String?,
            genre: String?,
            author: String?,
            price: Int?,
            rating: Int?
    ): Long {
        var id: Long = -1
        val entity = Book(
                name,
                description,
                genre,
                author,
                price,
                rating
        )

        em.persist(entity)

        if (entity.id != null) {
            id = entity.id!!
        }

        return id
    }

    override fun updateBook(
            name: String,
            description: String?,
            genre: String?,
            author: String?,
            price: Int?,
            rating: Int?,
            id: Long): Boolean {
        val book = em.find(Book::class.java, id) ?: return false

        if (name.isNullOrEmpty()) {
            return false
        }
        if(price != null && price < 1){
            return false
        }
        if(rating != null && rating < 1){
            return false
        }

        book.name = name
        book.description = description
        book.genre = genre
        book.author = author
        book.price = price
        book.rating = rating

        return true
    }

    override fun updatePrice(price: Int, id: Long): Boolean {
        val book = em.find(Book::class.java, id) ?: return false

        if (price == null || price < 0 || price > 1000) {
            return false
        }

        book.price = price

        return true
    }


}