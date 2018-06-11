package com.stella.bookexam.member.repository

import com.stella.bookexam.member.domain.model.Member
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
interface MemberRepository : CrudRepository<Member, String>, MemberRepositoryCustom {

    fun existsByUsername(username: String): Boolean

    fun findAllByUsername(username: String): Iterable<Member>
}

@Transactional
interface MemberRepositoryCustom {
    fun createMember(
            username: String,
            books: MutableMap<String, Int>,
            id: String
    ): Boolean

    fun updateMember(
            username: String,
            books: MutableMap<String, Int>,
            id: String): Boolean

    fun updateUsername(username: String, id: String): Boolean

    fun findByUsername(username: String): Member

    fun addBook(id: String, book: String, price: Int): Boolean

}

open class MemberRepositoryImpl : MemberRepositoryCustom {


    @PersistenceContext
    private lateinit var em: EntityManager

    override fun createMember(
            username: String,
            books: MutableMap<String, Int>,
            id: String
    ): Boolean {
        if (username.trim().isNullOrBlank() || username.length > 50 || id.isNullOrBlank()) {
            return false
        }
        val entity = Member(
                id,
                username,
                books,
                ZonedDateTime.now()
        )

        try {
            em.persist(entity)

        } catch (e: Exception) {
            return false
        }

        return true
    }

    override fun updateMember(username: String, books: MutableMap<String, Int>, id: String): Boolean {
        val entity = em.find(Member::class.java, id) ?: return false

        if (username.isNullOrEmpty() || username.length > 50) {
            return false
        }

        entity.username = username
        entity.books = books

        return true
    }

    override fun updateUsername(username: String, id: String): Boolean {
        val entity = em.find(Member::class.java, id) ?: return false

        if (username.isNullOrEmpty() || username.length > 50) {
            return false
        }

        entity.username = username

        return true
    }

    override fun findByUsername(username: String): Member {
        val query = em.createQuery("select m from Member m where m.username = ?1", Member::class.java)
        query.setParameter(1, username)
        return query.setMaxResults(1).singleResult
    }

    override fun addBook(id: String, book: String, price: Int): Boolean {
        try {
            val entity = em.find(Member::class.java, id)
            entity.books[book] = price
        } catch (e: Exception) {
            return false
        }
        return true

    }
}