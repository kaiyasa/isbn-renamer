
package org.miner.application.isbn

import groovy.json.*;

import org.miner.utils.JsonPersistence

class IsbnCache {
    IsbnCache(String cacheRoot) {
        this.cacheRoot = cacheRoot
    }

    def noneIsbn = '9999999999999'

    IsbnData find(String isbn) {
        if (isbn == noneIsbn) {
            def result = createIsbn(noneIsbn)
            result.title = 'No ISBNs found (type: u <isbn>  to specify one)'
            result.publishedDate = '9999-77-88'
            result.publisher = 'Dr. Evil Publishing'
            return result
        }

        try {
            return new IsbnData(storage(isbn).load())
        } catch (java.io.FileNotFoundException e) {
            return null
        }
    }

    IsbnData createIsbn(String isbn) {
        def detail = new IsbnData([:])
        detail.isbn13 = isbn
        return detail
    }

    def save(String isbn, content) {
        new File(cacheRoot).mkdirs()
        return new IsbnData(storage(isbn).save(content))
    }

    def storage(String isbn) {
        return new JsonPersistence(cachePath(isbn))
    }

    def cachePath(String key) {
        return "${cacheRoot}/${key}.json"
    }

    def cacheRoot
}
