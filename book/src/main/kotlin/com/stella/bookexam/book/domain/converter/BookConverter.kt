package com.stella.bookexam.book.domain.converter

import com.stella.bookexam.book.domain.model.Book
import com.stella.bookexam.schema.BookDto

class BookConverter {

    companion object {

        fun transform(entity: Book): BookDto {
            return BookDto(
                    name = entity.name,
                    description = entity.description,
                    genre = entity.genre,
                    author = entity.author,
                    price = entity.price,
                    rating = entity.rating,
                    id = entity.id.toString()
            )
        }

        fun transform(entities: Iterable<Book>): Iterable<BookDto> {
            return entities.map { transform(it) }
        }
    }
}