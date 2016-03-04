
package org.miner.application.isbn

class Loader {
    Loader(cache, service) {
        this.cache = cache
        this.service = service
    }

    Map load(String isbn) {
        return cache.find(isbn)
    }

    Map fetch(String isbn) {
        def raw = service.isbnFetch(isbn)

        return (raw['totalItems'] > 0) ?
            cache.save(isbn, raw) :
            cache.createIsbn(isbn).with {
                title = "$isbn not found"
            }
    }

    Map gather(String seek) {
        if (!seek || seek.size() != 13)
            throw new IllegalArgumentException('invalid ISBN-13')

        def detail = load(seek)
        if (!detail) 
            detail = fetch(seek)

        def data = [isbn: seek]
        return populate(data, detail)
    }

    List gather(List<String> isbnList) {
        return isbnList.collect { String code -> gather(code) }
    }

    Map populate(Map data, isbnData) {
        return data?.with {
            detail = isbnData

            (title, edition) = titleClean(detail.title)
            base = edition ? "$title, $edition Ed" : title
            year = toYear(detail.publishedDate)
            publisher = mapPublisher(detail.publisher, isbn)
        }
    }

    def files(dir, prefix, isbn) {
        return filer.fileGroup(dir, prefix, isbn)
    }

    def cache, service
    def filer = New Filer()
}
