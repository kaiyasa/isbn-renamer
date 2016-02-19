
package org.miner.application.isbn

class Publisher {
    def filePath
    Publisher(String path) {
        filePath = path
        map = new File(path).text.split("\n") \
                .collect { it.split('\\|') }.inject([:], { r, v -> r += [ (v[0]) : v[1]] } )
    }

    String map(String key) {
        return map[key]
    }

    def add(String key, String value) {
        map[key] = value
    }

    def save() {
        new File(filePath).text = map.collect { k, v -> "$k|$v\n" }.join('')
    }

    def map
}
