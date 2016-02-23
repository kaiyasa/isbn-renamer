
package org.miner.application.isbn

class DisplayFormatter {
    def render(int lwidth, int twidth, String label, Object item, String sep = ': ') {
        printf("%${lwidth}s%s%${twidth}s", label, sep, item.toString())
        return System.out
    }

    def render(int lwidth, int fwidth, Map fields) {
        fields.each { label, text ->
            render(lwidth, fwidth, label, text)
        }
        return System.out
    }

    def render(List<Integer> limits, List[] lists) {
        if (limits.size() != lists.size())
            throw new IllegalArgumentException('size of limits and lists must match')

        def items = lists
        while (!items*.isEmpty().inject(false) { r, v -> r || v }) {
            def entries = items*.take(1)*.with { it ? get(0) : '' }
            items = items*.drop(1)
            [limits, entries].transpose().each { limit, entry ->
                printf("%${limit}s", entry?.toString() ?: '' )
            }
            println()
        }
        return System.out
    }

    def left(int margin, List items) {
        return items.collect { left(margin, it) }
    }

    def left(int margin, item) {
        return sprintf("%${margin}s%s", '', item?.toString() ?: '')
    }
}
