
package org.miner.application.isbn

import groovy.io.FileType

import org.apache.commons.validator.routines.ISBNValidator;
import org.miner.utils.ConfigFile

class IsbnRenamer {
    static void main(args) {

        String.metaClass.times { n ->
            return delegate.with { base ->
                (1..n).collect {base}.join('')
            }
        }

        if (!args) {
            println("isbnrenamer collectionName <dirs|files>...")
            System.exit(0)
        }

        def appHome = System.properties['user.home'] + '/.isbnrenamer'
        System.setProperty('app.home', appHome)

        proxySetup(System.getenv('http_proxy'), 'http')
        proxySetup(System.getenv('https_proxy'), 'https')

        def (collectionName) = args.take(1)
        new IsbnRenamer(new ConfigFile(appHome + '/config.yml')) \
          .run(collectionName, args.drop(1))
    }

    static void proxySetup(String proxyUrl, String protocol) {
        def proxy = proxyUrl ? new URI(proxyUrl) : null

        if (!proxy || !proxy.host)
            return

        System.setProperty(protocol + '.proxyHost', proxy.host)
        System.setProperty(protocol + '.proxyPort', proxy.port.toString())
    }

    def cache
    def pubmap
    def config
    def service
    def out = new DisplayFormatter()

    def suffix = ~/(\s|[,.-])*$/
    def eds = [
        '2nd': ~/(?i)\(?(2|2nd|2th|second|two)\s*ed(ition)?\)?/
      , '3rd': ~/(?i)\(?(3|3rd|3th|third|three)\s*ed(ition)?\)?/
      , '4th': ~/(?i)\(?(4|4rd|4th|fourth|four)\s*ed(ition)?\)?/
      , '5th': ~/(?i)\(?(5|5rd|5th|fifth|five)\s*ed(ition)?\)?/
    ]

    IsbnRenamer(config) {
        this.config = config
        service = new GoogleBooks(config.values.services.googlebooks)

        (pubmap, cache) = config.values.locations.with {
            [
                new Publisher(publisherMap)
              , new IsbnCache(cacheDir)
            ]
        }
    }

    def run(toUse, args) {
/*
        if (!config.containsKey(toUse)) {
            println("Unknown collection '$toUse'")
            System.exit(-1)
        }
*/

        def (shelfDir, holdDir) = config.values.collections[toUse].with {
            [shelfDir, holdDir]
        }

        def files = []
        for(name in args) {
            def path = new File(name)

            if (!path.exists())
                continue

            if (path.isFile())
                files += name
            else if (path.isDirectory()) {
                path.eachFileRecurse(FileType.FILES) { file ->
                    files += file.path
                }
            } else
                println("WARN: unknown filesystem object '$name'")
        }

        files.eachWithIndex { name, i ->
            if (new File(name).exists()) {
                println("=".times(80))
                println("Processing #${i+1} of ${files.size()}\n")
                perform(name, shelfDir, holdDir)
            }
        }
    }

    def perform(String src, String shelfDir, String holdDir) {
        def (dir, base, ext) = parsePath(src)
        def fileName = "${dir}/${base}.pdf"
        def isbnList = validate(parse(annotate(base, convert(fileName)))).unique()

        boolean prompting = true, full = false
        List manual = []
        while (prompting) {
            def choice = (manual + isbnList).with { work ->
                if (work.size() == 0)
                    work += '9999999999999'

                 def elements = 0 .. (full ? Math.min(4, work.size() - 1) : 0)
                 return work[elements].collect { isbnDetail(it) }
            }

            display(full, base, choice)
            print('Choice (? = help): ')

            def (ans, option) = input(System.console())
            def title = base
            def dest = shelfDir
            try {
                if (ans == 'h') {
                    dest = holdDir
                    title = base
                } else {
                    def allowed = 0 .. choice.size() - 1
                    int item = (ans == 'y') ? 0 : Integer.parseInt(ans)
                    if (!(item in allowed)) {
                        println("\n  Invalid item number, use ${allowed.inspect()}")
                        continue
                    }
                    title = choice[item].name
                }

                move(fileGroup(dir, base), title, dest)
                return
            } catch (NumberFormatException e) {
                if (ans == 'n')
                    full = true
                else if (ans == 's')
                    prompting = false
                else if (ans == 'u') {
                    manual = [option]
                    full = false
                } else if (ans == 'set') {
                    def detail = cache.find(choice[0].isbn)
                    def aliases = [date: 'publishedDate', length: 'runLength',
                                   time: 'runLength', pages: 'pageCount', pub: 'publisher']
                    def (property, value) = parseSetting(option, aliases)

                    try {
                        if (detail == null)
                            detail = cache.createIsbn(choice[0].isbn)

                        detail[property] = value
                    } catch (MissingPropertyException e2) {
                        println("\nInvalid property '$property'")
                        e2.printStackTrace()
                        continue
                    }
                    detail.root['userEdited'] = true
                    cache.save(choice[0].isbn, detail.root)
                    full = false
                } else if (ans == 'm') {
                    def (oldName, newName) = parseSetting(option, [:])
                    pubmap.add(oldName.toLowerCase(), newName)
                    pubmap.save()
                } else if (ans == '?')
                    help()
            }
        }
    }

    def help() {
        println('''
   <Item #>     - e.g. Choice (? = help): 2
   (y)es        - selects 'To'  (a.k.a tem 0)
   (n)o         - reject 'To' and list the next ISBNs
   (h)old       - move to a holding directory with name unchanged
   (s)kip       - skip and leave as found
   (u)se <isbn> - provide an ISBN manually

   (m)ap <original> = <new>
       map 'original' publisher to 'new' and remember it
   (set) <property> = <value>  - set properties on item 0
       e.g. set title = My Better Title
''')
    }

    def move(fileList, target, dest) {
        new File(dest).mkdirs()
        fileList.each { file ->
            def (dir, base, ext) = parsePath(file.path)
            def name = "${dest}/${target}${ext}"

            for(it in (0..1000)) {
                if (it == 1000)
                    throw new RuntimeException("more than 1000 versions of '${dest}/${name}'")

                def location = it ? "${name}.~${it}~" : "$name"

                if (!new File(location).exists()) {
                    file.renameTo(location)
                    break
                }
            }
        }
    }

    List input(reader) {
        String line = reader.readLine()
        // similar to bash:  read var rest < /dev/tty
        def parts = line.trim().split(' ')
        return [parts.take(1)[0], parts.drop(1).join(' ').trim()]

    }

    List parseSetting(String line, Map aliases) {
        def parts = line.split('=')
        def (property) = parts.take(1)*.trim()
        def value = parts.drop(1).join('=').trim()
        return [aliases[property] ?: property, value]
    }

    Map isbnDetail(String seek) {
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

    def toYear(String date) {
        return date[0..3]
    }

    def mapPublisher(name, isbn) {
        def result = pubmap.map( (name ?: "").toLowerCase())

        if (result)
            return result

        def keys = (5..11).collect { isbn[3..it] }
        for(String key : keys) {
            if ((result = pubmap.map(key)))
                return result
        }
        return name
    }

    def titleClean(name) {
        def edition

        for(ed in eds) {
            def old = name
            name = name - ed.value
            if (old != name) {
                edition = ed.key
                break
            }
        }

        return [name - suffix, edition]
    }

    def width = [
        single: [4, 1]
      , field:  [11, -17]
      , columns: [-30, -30]
    ]

    def display(boolean full, src, list) {
        out.render(*width.single, 'From', src).println()

        if (full && list.size() > 1) {
            list.drop(1).eachWithIndex { entry, i ->
                out.render(*width.single, i + 1, entry.name, ') ').println()
            }
            return
        }

        def item = list[0]
        out.render(*width.single, 'To', item.base).println('\n')

        if (!item.detail)
            return

        out.render(*width.field, ['Using ISBN': item.isbn, 'Group': 'not impl']).println()
        out.render(*width.field, ['Year': item.year] +
            ((item.detail.kind == 'video') ? ['Length': item.detail.runLength]
                                           : ['Pages': item.detail.pageCount])
        ).println()
        out.render(*width.field, ['Publisher': item.publisher]).println()

        out.render(*width.field, ['Author(s)': '', 'Categories': '']).println()
        out.render(width.columns, out.left(4, item.detail.authors),
                                  out.left(3, item.detail.categories)).println('\n')
    }

    List parsePath(String path) {
        def dir = '.', base = '', ext = ''

        if (!path)
            return [dir, base, ext]

        if (path in ['.', '..'])
            return [path, base, ext]

        path = path.replaceAll( /\/\/+/, '/')
        def from = path.lastIndexOf('/')

        // directory present
        if (from > -1)
            dir = path[0 ..< from] ?: '/'

        base = path.drop(1 + from)
        def to = base.lastIndexOf('.')

        // no extension present
        if (to == -1 || to == base.size() - 1)
            return [dir, base, ext]

        return [dir, base[0 ..< to], base[to .. -1]]
    }

    List fileGroup(String dir, String prefix) {
        def result = []
        new File(dir).eachFileMatch( { it.startsWith(prefix) } ) {
            result << it
        }
        return result
    }

    String convert(String fname) {
        def command = ['pdftotext', fname, '-']

        return command.execute().text
    }

    String annotate(String extra, String text) {
        return "  $extra\n  $text"
    }

    List parse(String text) {
        def isbnPatterns = ~/\s((\d{9}([xX]|\d))|\d{13})\s/

        return text.replaceAll('-', "").findAll(isbnPatterns)*.trim()
    }

    List validate(List isbnList) {
        return isbnList.inject([]) { r, v ->
            def isbn = checkIsbn(v)

            return isbn ? r += isbn : r
        }
    }

    // converts to ISBN13 as well
    def code = ISBNValidator.getInstance(true);
    String checkIsbn(String text) {
        return code.validate(text)
    }
}
