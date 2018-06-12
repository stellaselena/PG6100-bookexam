package com.stella.bookexam.store.repository

import com.stella.bookexam.store.domain.model.BookForSale
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
interface StoreRepository : CrudRepository<BookForSale, Long>, StoreRepositoryCustom {
    fun findAllByName(name: String): Iterable<BookForSale>
    fun findAllByPrice(price: Int): Iterable<BookForSale>
    fun findAllBySoldBy(soldBy: String): Iterable<BookForSale>
}

@Transactional
interface StoreRepositoryCustom {
    fun createBookForSale(
            name: String,
            soldBy: String,
            price: Int) : Long
    fun update(
            name: String,
            soldBy: String,
            price: Int,
            id: Long) : Boolean
    fun getLast10PostedBooksForSale() : Iterable<BookForSale>

}

open class StoreRepositoryImpl : StoreRepositoryCustom {

    @PersistenceContext
    private lateinit var em: EntityManager

    override fun  createBookForSale(
            name: String,
            soldBy: String,
            price: Int): Long {
        var id = -1L

        val entity = BookForSale(
                name,
                soldBy,
                price,
                null,
                ZonedDateTime.now()

        )
        em.persist(entity)

        if (entity.id != null) id = entity.id!!

        return id
    }

    override fun update(
            name: String,
            soldBy: String,
            price: Int,
            id: Long): Boolean {
        val entity = em.find(BookForSale::class.java, id) ?: return false

        if (
                name.isNullOrBlank() ||
                soldBy.isNullOrBlank() ||
                price < 1)
            return false

        entity.name = name
        entity.soldBy = soldBy
        entity.price = price

        return true
    }

    override fun getLast10PostedBooksForSale() : Iterable<BookForSale>{
        val query = em .createQuery("select b from BookForSale b order by b.createdOn DESC", BookForSale::class.java)
        return query.setMaxResults(10).resultList.toList()
    }


}