package com.stella.bookexam.member.repository

import com.stella.bookexam.member.domain.model.Member
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
class MemberRepositoryImplTest {

    @Autowired
    private lateinit var repo: MemberRepository

    @Before
    fun setup() {
        Assert.assertEquals(0, repo.count())
    }

    /**Create a valid member**/
    @Test
    fun testCreateMember_Valid() {
        val member = getValidMembers()[0]

        val savedId = repo.createMember(
                member.username,
                member.books,
                member.id
        )

        Assert.assertEquals(1, repo.count())

        val foundMember = repo.findOne(member.id)
        foundMember.username = member.username
    }

    /**Try to create a member with invalid username constraint**/
    @Test
    fun testCreateMember_Invalid() {
        val member = getValidMembers()[0]

        member.username = " "
        val savedMember = repo.createMember(
                member.username,
                member.books,
                member.id
        )
        Assert.assertEquals(false, savedMember)
    }

    /**Find all members by a given username**/
    @Test
    fun testFindMemberByUsername() {
        val member1 = getValidMembers()[0]
        val member2 = getValidMembers()[1]

        createMember(member1)
        createMember(member2)

        Assert.assertEquals(2, repo.count())
        val membersFound = repo.findAllByUsername(member1.username)

        Assert.assertEquals(1, membersFound.count())
        Assert.assertTrue(membersFound.any({ e -> e.username == member1.username }))

    }

    /**Update a member**/
    @Test
    fun testUpdateMember() {
        val member1 = getValidMembers()[0]
        val member2 = getValidMembers()[1]

        createMember(member1)

        Assert.assertEquals(1, repo.count())

        val foundMember = repo.findOne(member1.id)

        val wasSuccessful = updateMember(member2, mutableMapOf("Book1" to 15, "Book3" to 10), foundMember.id)
        Assert.assertEquals(true, wasSuccessful)


        Assert.assertEquals(foundMember?.username, member2.username)
        Assert.assertEquals(foundMember?.id, member1.id)

        Assert.assertEquals(1, repo.count())
    }


    /**Try to update a member with invalid id**/
    @Test
    fun testChangeIdInUpdate_Invalid() {
        val member1 = getValidMembers()[0]
        val member2 = getValidMembers()[1]

        createMember(member1)
        repo.findOne(member1.id)

        member2.id = "24242424"
        updateMember(member2, mutableMapOf("Book1" to 15, "Book3" to 10),member1.id)

        // Validate that id did not change
        val foundMember = repo.findOne("24242424")
        Assert.assertNull(foundMember)
        Assert.assertEquals(1, repo.count())
    }

    /**Delete a member**/
    @Test
    fun testDeleteMember() {
        val member1 = getValidMembers()[0]
        val member2 = getValidMembers()[1]

        createMember(member1)
        createMember(member2)

        val foundMember1 = repo.findOne(member1.id)
        val foundMember2 = repo.findOne(member2.id)

        Assert.assertEquals(2, repo.count())
        repo.delete(foundMember1.id)

        Assert.assertEquals(1, repo.count())

        repo.delete(foundMember2.id)
        Assert.assertEquals(0, repo.count())
    }

    /**Try to delete a non existing member**/
    @Test
    fun testDeleteWhenNoMemberExists_Invalid() {
        Assert.assertEquals(0, repo.count())
        try {
            repo.delete("24242424")
            Assert.fail("Delete id that doesnt should throw exception")
        } catch (e: Exception) {

        }
        Assert.assertEquals(0, repo.count())
    }

    /**Check if a member exists by username**/
    @Test
    fun testExistsByUsername() {
        val member1 = getValidMembers()[0]
        val member2 = getValidMembers()[1]
        Assert.assertEquals(false, repo.existsByUsername(member1.username))

        createMember(member1)
        Assert.assertEquals(true, repo.existsByUsername(member1.username))

        Assert.assertEquals(false, repo.existsByUsername(member2.username))
    }

    /**Try to create a member with invalid username constraint**/
    @Test
    fun testUsernameConstraint_Invalid() {
        var savedId = true
        val member = getValidMembers()[0]
        member.username = "invalidUsername".repeat(20)

        try {
            savedId = createMember(member)
        } catch (e: Exception) {
        }
        Assert.assertEquals(false, savedId)

        member.username = ""
        try {
            savedId = createMember(member)
        } catch (e: Exception) {
        }

        Assert.assertEquals(false, savedId)
    }

    /**Add books and their price that a member offers for sale**/
    @Test
    fun testAddBooks() {
        val member = getValidMembers()[0]
        createMember(member)
        repo.addBook(member.id, "Book 1", 5)
        repo.addBook(member.id, "Book 2", 20)
        repo.addBook(member.id, "Book 3", 15)

        val foundMember = repo.findOne(member.id)
        Assert.assertEquals(5, foundMember.books.size)
    }

    fun getValidMembers(): List<Member> {
        return listOf(
                Member(
                        "foo",
                        "44",
                        mutableMapOf("Book1" to 5, "Book2" to 10)
                ),
                Member(
                        "bar",
                        "46",
                        mutableMapOf("Book3" to 15, "Book4" to 20)
                ),
                Member(
                        "admin",
                        "96",
                        mutableMapOf("Book1" to 15, "Book3" to 10)
                )
        )
    }


    fun createMember(member: Member): Boolean {
        val savedId = repo.createMember(
                member.username,
                member.books,
                member.id
        )


        return savedId
    }


    fun updateMember(member: Member, books: MutableMap<String, Int>, id: String): Boolean {
        return repo.updateMember(
                username = member.username,
                books = member.books,
                id = id
        )
    }
}