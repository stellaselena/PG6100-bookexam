package com.stella.bookexam.store.domain.converter

import com.stella.bookexam.schema.BookForSaleDto
import com.stella.bookexam.store.domain.model.BookForSale

class BookForSaleConverter {
    companion object {
        fun transform(entity: BookForSale): BookForSaleDto {
            return BookForSaleDto(
                    id = entity.id?.toString(),
                    name = entity.name,
                    soldBy = entity.soldBy,
                    price = entity.price,
                    createdOn = entity.createdOn

            )
        }

        fun transform(entities: Iterable<BookForSale>): Iterable<BookForSaleDto> {
            return entities.map { transform(it) }
        }
    }
}