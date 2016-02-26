
package org.miner.application.isbn

class Interaction {
    Interaction(reader, writer, Closure loadCode) {
        this.reader = reader
        this.loader = loadCode
        this.out    = new DisplayRender(writer)
    }

    def action = [y: 'move', h: 'hold', s: 'skip']

    def prompt(String path, List isbnList) {
        def (dir, base, ext) = parsePath(path)
        def cmd, options, text
        boolean prompt = true

        def choice = loader.isbn(isbnList)
        def selected = choice[0]
        selected.files = loader.files(selected)

        while (prompt) {
            display(base, selected)

            (cmd, options) = input(1)
            text = cmd.replaceAll(/-/, '')

            if (cmd in ['y', 'h', 's'])
                prompt = false
            else if (cmd in ['m', 'set'])
                alterItem(selected, cmd, options)
            else if (text.isInteger() || likelyIsbn(text))
                selected = selectItem(choice, text) ?: selected
            else
                miscCommands(cmd, options)
        }
        return [action[cmd], (cmd == 's' ? null : selected)]
    }

    def display(from, item) {
        boolean isVideo = item?.detail?.kind == 'video'

        out {
            field(4) \
                << ['From', from] << endl \
                << ['To', item.base] << endl << endl

            def res = field(11, -17) \
                << ['Using ISBN', item.isbn] \
                << ['Group', item.files.groups] << endl

            if (!item?.detail)
                return

            def detail = item.detail
            res << ['Year', item.year] \
                << (isVideo ? ['Length', detail.runLength] 
                             : ['Pages', detail.pageCount]) << endl \
                << ['Publisher', item.publisher] << endl \
                << ['Author(s)', '']  \
                << ['Categories', ''] << endl \
                << table([-30, -30], left(4, detail.authors), left(3, detail.categories))

            out << endl << 'Choice (? = help): '
        }
    }

    boolean likelyIsbn(String text ) {
        for(regex in [ ~/^\d{13}$/, ~/^\d{9}(\d|[Xx])$/ ])
            if ((text =~ regex).matches())
                return true
        return false
    }

    def selectItem(List choice, String cmd) {
        def allowed = (1 ..< choice.size())
        def selected = null

        if (cmd.size() in [10, 13])
            selected = loader.isbn(cmd)
        else {
            int item = cmd.toInteger()
            if (item in allowed)
                selected = choice[item]
            else
                println("invalid item # (use ${allowed.inspect()})")
        }
        return selected
    }

    def aliases = [:].withDefault { [:] }

    void alterItem(selected, String cmd, String options) {
        def (first, second) = parseSetting(aliases[cmd], options)

        if (cmd == 'set') {
            try {
                selected.detail[first] = second
                selected.detail.root.userEdited = true
                selected.detail = loader.save(selected.isbn, selected.detail.root)
            } catch (MissingPropertyException mpe) {
                println("invalid property name '${first}'")
            }
        } else {
            loader.publishers.add(first, second)
            loader.publishers.save()
            selected.publisher = mapPublisher(selected.detail.publisher)
        }
    }

    void miscCommands(String cmd, String options) {
        if (cmd == "?")
            help()
        else
            println("invalid command '$cmd'")
    }

    List input(int count = 1) {
        String line = reader.readLine()
        return readWords(count, line)
    }

    // Whitespace handling similar to bash behavior for 'read'
    List readWords(int count, String line) {
        if (count < 1)
            throw new IllegalArgumentException("count must be 1 or more words")

        def word, words = parseWords(line)
        def result = (1 .. count).collect {
            word  = words[1] ?: ''
            words = words.drop(2)
            return word
        }
        return result << words.join('').trim()
    }

    List parseWords(String text) {
        return text?.split(/\b/) ?: []
    }

    List parseSetting(Map aliases, String text) {
        int pos = text?.indexOf(/=/) ?: -1

        if (pos in [-1, 0, (text?.length() ?: 0) - 1])
            throw new UserInputException('syntax error, proper form is target=my value')

        def target = text[0 ..< pos].trim()
        return [aliases[target] ?: target, text[(pos + 1) .. -1]]*.trim()
    }

    def reader, loader, out
}
