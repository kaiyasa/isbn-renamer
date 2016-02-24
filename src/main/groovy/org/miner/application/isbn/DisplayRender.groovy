
package org.miner.application.isbn

class DisplayRender {
    static def defaults = [
        separator: [
             field: ': '
          , column: ''
        ]
    ]

    DisplayRender() {
        this(defaults)
    }

    DisplayRender(Map options, Map overrides = [:]) {
        this.options = (options + overrides)
    }

    DisplayRender call(Closure code) {
        code( code.delegate = this )
        return this
    }

    DisplayRender leftShift(List item) {
        def (dlabel, dtext) = item
        def (w1, w2) = options?.field?.width?.with {
            [label ?: 1, text  ?: 1]
        } ?: [1, 1]

        this << sprintf("%${w1}s%s%${w2}s", dlabel,
            options?.separator?.field ?: '', dtext?.toString())

        return this
    }

    DisplayRender leftShift(Table table) {
        this << table.toString(options)
        return this
    }

    DisplayRender leftShift(item) {
        print(item?.toString())
        return this
    }

    DisplayRender field(int label = 1, int text = 1) {
        return new DisplayRender(options, [field: [width: [label: label, text: text]]])
    }

    DisplayRender separator(String text = ': ') {
        return new DisplayRender(options, [separator: [field: text]])
    }

    DisplayRender column(String text = '') {
        return new DisplayRender(options, [separator: [column: text]])
    }

    Table table(List limits, List[] items) {
        return new Table(limits, items)
    }

    List left(int pad, List data) {
        return data?.collect { left(pad, it) } ?: []
    }

    String left(int pad, item) {
        return sprintf("%${pad}s%s", '', item?.toString())
    }

    String endl = '\n'
    Map options

    class Table {
        Table(List widths, List[] items) {
            if (items.size() != widths.size())
                throw new IllegalArgumentException('widths and items must match by count')

            data = [widths, items]
        }
  
        String toString(Map options) {
            def (widths, items) = data
            def result = []

            while (items*.isEmpty().any { it == false }) {
                def entries = items*.take(1)*.with { it ? it.get(0) : '' }

                result += [widths, entries].transpose() \
                  .collect { format(*it) }.join(options?.separator?.column ?: '')

                items = items*.drop(1)
            }
            return result.join('\n')
        }

        String format(int width, String text) {
            return sprintf("%${width}s", text)
        }

        List data
    }
}
