
package org.miner.application.isbn

class IsbnData {
    IsbnData(root) {
        if (root == null)
            throw new IllegalArgumentException("null root for ISBN data")

        this.root = root

        // build out structure if empty
        if (root.items == null)
            root.items = [[:]]

        if (root.items[0].volumeInfo == null)
            root.items[0].volumeInfo = [:]

        detail = root.items[0].volumeInfo
    }

    def getTitle() {
        return detail.title
    }

    def setTitle(value) {
        return detail.title = value
    }

    def getPublisher() {
        return detail.publisher
    }

    def setPublisher(value) {
        return detail.publisher = value
    }

    def getPublishedDate() {
        return detail.publishedDate ?: '9999'
    }

    def setPublishedDate(value) {
        return detail.publishedDate = value
    }

    def findIndustryId(String type) {
        return detail.industryIdentifiers.find { it.type == type }
    }

    def getIsbn13() {
        return findIndustryId("ISBN_13")?.identifier
    }

    def setIsbn13(String value) {
        if (value.length() != 13)
            throw new IllegalArgumentException("ISBN-13 required to have 13 digits")

        if (detail.industryIdentifiers == null)
            detail.industryIdentifiers = []

        def entry = findIndustryId("ISBN_13")
        if (entry == null)
            detail.industryIdentifiers += (entry = [type: 'ISBN_13'])

        entry.identifier = value
        return value
    }

    def getPageCount() {
        return detail.pageCount ?: 0
    }

    def setPageCount(value) {
        return detail.pageCount = Integer.parseInt(value.toString())
    }

    List getAuthors() {
        return detail.authors ?: []
    }

    List setAuthors(String value) {
        return detail.authors = value.split(';')*.trim()
    }

    List getCategories() {
        return detail.categories ?: []
    }

    List setCategories(String value) {
        return detail.categories = value.split(';')*.trim()
    }

    def root, detail
}
