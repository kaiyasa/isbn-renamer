
package org.miner.application.isbn

class Loader {
    Loader(cache) {
        this.cache = cache
    }

    def isbn(List isbnList) {
        return isbnList.collect { code -> isbn(code) }
    }

    Map isbn(String seek) {
        if (!seek || seek.size() != 13)
            throw new IllegalArgumentException('invalid ISBN-13')

        def resource = this
        return [isbn: seek].with { result ->
            detail = resource.cache.find(isbn)

            if (!detail) {
                def raw = resource.service.isbnFetch(isbn)

                base = "$isbn not found"
                if (raw['totalItems'] > 0)
                    detail = cache.save(isbn, raw)
                else
                    return result
            }

            (title, edition) = titleClean(detail.title)
            base = edition ? "$title, $edition Ed" : title
            year = toYear(detail.publishedDate)
            publisher = mapPublisher(detail.publisher, isbn)

            name = "$base [$publisher, $isbn, $year]"
            return result
        }
    }

    def files(dir, prefix, isbn) {
        return filer.fileGroup(dir, prefix, isbn)
    }

    def cache
    def filer = New Filer()
}
