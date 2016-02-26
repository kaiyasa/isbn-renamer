
package org.miner.application.isbn

class Processor {
    Processor(String shelfDir, String holdDir) {
        targetDir.with {
            move = shelfDir
            hold = holdDir
        }
    }

    def perform(String path) {
        def (action, item) = interact.prompt(path)

        if (action == 'skip')
            return

        move(action, item)
    }

    def move(action, item) {
        def destDir = targetDir[action]

        for(src in item.files) {
            def destPath = "$destDir/${targetFile(action, item, src)}"

            if (!moveFile(src.path, destPath))
                println("ERROR: failed to move ${src.path}")
        }
    }

    boolean moveFile(srcPath, destPath) {
        def src = new File(src.path)
        def targets = (1 ..< 1000).inject([destPath]) { c, i ->
            c << sprintf('$s.~%03d~', destPath, i)
        }

        for(path in targets) {
            def target = new File(path)
            if (!target.exists())
                return src.renameTo(target.path)
        }
        throw new IllegalStateException("ERROR: more than 1000 backups of $destPath")
    }

    def targetFile(action, item, src) {
        if (action == 'hold') {
            def (dir, base, ext) = interact.parsePath(src.path)
            return "$base$ext"
        }

        def kind   = src.category.with { it != 'Book' ? " ($it)" : '' }
        def detail = [item.publisher, item.isbn, item.year].join(', ')

        return "${item.base}${kind} [${detail}]"
    }

    Interation interact = new Interation()
    def targetDir = [:].withDefault {
        throw new IllegalArgmentException("invalid target: $it")
    }
}
