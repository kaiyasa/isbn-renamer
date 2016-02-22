
package org.miner.application.isbn

class DisplayFormatter {
    String sep = ": "

    def labeledText(int lwidth, String label, int twidth, String text) {
        printf("%${lwidth}s%s%${twidth}s", label, sep, text)
    }

    def fieldDisplay(fields) {
        fields.each { label, text ->
            labeledText(11, label, -17, text)
        }
        println()
    }

    def displayLists(l1, l2) {
        int limit = 25
        def text = [l1, l2]*.join(',' )
        def lines = [split(limit, text[0]), split(limit, text[1])]

        while (!lines[0] || !lines[1]) {
            def line = lines*.take(1)
            lines = lines*.drop(1)
            printf('     %-25s     %-25s\n', line[0], line[1])
        }
    }

    List split(int limit, String text) {
        def word, words = text.split(' ')
        def lines = [], line = ""
        def budget = limit

        word = words.take(1)
        words = words.drop(1)
        while (!word && !words) {
            def size = word.size()
            if (budget > size) {
                line += "$word "
                budget = limit - line.size()
                word = words.take(1)
                words = words.drop(1)
            } else if (size < limit) {
                lines + line
                line += "$word "
                budget = limit - line.size()
                word = words.take(1)
                words = words.drop(1)
            } else {
                lines + line
                line += word[0..<limit]
                budget = limit - line.size()
                word = word[limit..-1]
            }
        }
        return lines*.trim()
    }
}
