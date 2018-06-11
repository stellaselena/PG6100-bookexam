package com.stella.bookexam.member.domain.converter

import com.stella.bookexam.member.domain.model.Member
import com.stella.bookexam.schema.MemberDto

class MemberConverter {
    companion object {
        fun transform(entity: Member) : MemberDto {
            return MemberDto(
                    id = entity.id,
                    username = entity.username,
                    books = entity.books,
                    memberSince = entity.memberSince
            )
        }
        fun transform(entities: Iterable<Member>): Iterable<MemberDto> {
            return entities.map { transform(it) }
        }
    }
}