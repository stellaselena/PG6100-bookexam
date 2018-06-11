package com.stella.bookexam.gateway

import com.stella.bookexam.gateway.domain.model.UserEntity
import com.stella.bookexam.gateway.repository.UserRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private lateinit var repo: UserRepository

    @Before
    fun setup() {
        Assert.assertEquals(0, repo.count())
    }

    @Test
    fun createUser() {
        val user = UserEntity("name","password")

        repo.save(user)

        Assert.assertEquals(1, repo.count())
    }

    @Test
    fun createUser_Custom() {
        // Act
        val user0 = UserEntity("name","password")
        repo.save(user0)

        // Assert
        Assert.assertEquals(1, repo.count())
    }

    @Test
    fun findUserByUserName(){
        // Arrange
        val user0 = UserEntity("name","password")
        repo.save(user0)

        // Act
        val user = repo.findUserByUsername("name")
        // note: do not throw exception
        val user1 = repo.findUserByUsername("notExist")

        // Assert
        Assert.assertNull(user1)
        Assert.assertEquals("password", user!!.password)
    }
}