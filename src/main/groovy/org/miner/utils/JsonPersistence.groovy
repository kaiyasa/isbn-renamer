package org.miner.utils

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class JsonPersistence {
    JsonPersistence(String filePath) {
        path = filePath
    }

    def save(Object content) {
        new File(path).write(new JsonBuilder(content).toPrettyString())
        return content
    }

    def load() {
        return new JsonSlurper().parseText(new File(path).text)
    }

    def path
}
