
package org.miner.application.isbn

class Filer {
    def extensions = [
        [category: 'Book',  pattern: ~/\.pdf$/ ]
      , [category: 'Book',  pattern: ~/\.epub$/ ]
      , [category: 'Book',  pattern: ~/\.mobi$/ ]
      , [category: 'Code',  pattern: ~/\_code.zip$/ ]
      , [category: 'Video', pattern: ~/\[_ ]\[Video\].zip$/ ]
      , [category: 'Other', pattern: ~/\.[^.]+$/ ]
    ]

    List fileGroup(String dir, String prefix, String isbn) {
        def filter = { it.with { startsWith(prefix) || contains(isbn) } }

        def result = []
        new File(dir).eachFileMatch(filter) { file ->
            int prev = result.size()
            for(xx in extensions) {
                if (result.size() != prev)
                    break

                (file.name =~ xx.pattern).each { partial ->
                    result << [category: xx.category, dir: dir,
                               base: file.name - xx.pattern, ext: partial]
                }
            }
        }
        return result
    }
}
