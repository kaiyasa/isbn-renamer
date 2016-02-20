[1mdiff --git a/src/main/groovy/org/miner/application/isbn/IsbnData.groovy b/src/main/groovy/org/miner/application/isbn/IsbnData.groovy[m
[1mindex 153926d..bdd5d02 100644[m
[1m--- a/src/main/groovy/org/miner/application/isbn/IsbnData.groovy[m
[1m+++ b/src/main/groovy/org/miner/application/isbn/IsbnData.groovy[m
[36m@@ -89,5 +89,33 @@[m [mclass IsbnData {[m
         return detail.categories = value.split(';')*.trim()[m
     }[m
 [m
[32m+[m[32m    String getKind() {[m
[32m+[m[32m        return detail.kind ?: ""[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    String setKind(String value) {[m
[32m+[m[32m        return detail.kind = value[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /** in minutes */[m
[32m+[m[32m    int getRunLength() {[m
[32m+[m[32m        return detail.runLength ?: 0[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    int parseInt(value) {[m
[32m+[m[32m        return Integer.parseInt(value.toString())[m
[32m+[m[32m    }[m
[32m+[m[32m    int setRunLength(value) {[m
[32m+[m[32m         def result = (value ? value : '0').split(':').collect { parseInt(it) }.with {[m
[32m+[m[32m             switch (it.size()) {[m
[32m+[m[32m                 case 1: return it[0][m
[32m+[m[32m                 case 2: return it[0] * 60 + it[1][m
[32m+[m[32m                 case 3: return it[0] * 60 + it[1][m
[32m+[m[32m                 default: throw new IllegalArgumentException("unknown time format: $value")[m
[32m+[m[32m             }[m
[32m+[m[32m         }[m
[32m+[m[32m        return detail.runLength = result[m
[32m+[m[32m    }[m
[32m+[m
     def root, detail[m
 }[m
[1mdiff --git a/src/main/groovy/org/miner/application/isbn/IsbnRenamer.groovy b/src/main/groovy/org/miner/application/isbn/IsbnRenamer.groovy[m
[1mindex 564a0ee..3d7bf71 100644[m
[1m--- a/src/main/groovy/org/miner/application/isbn/IsbnRenamer.groovy[m
[1m+++ b/src/main/groovy/org/miner/application/isbn/IsbnRenamer.groovy[m
[36m@@ -84,7 +84,7 @@[m [mclass IsbnRenamer {[m
     def perform(String src, String shelfDir, String holdDir) {[m
         def (dir, base, ext) = parsePath(src)[m
         def fileName = "${dir}/${base}.pdf"[m
[31m-        def isbnList = validate(parse(convert(fileName))).unique()[m
[32m+[m[32m        def isbnList = validate(parse(annotate(base, convert(fileName)))).unique()[m
 [m
         boolean prompting = true, full = false[m
         List manual = [][m
[36m@@ -228,11 +228,20 @@[m [mclass IsbnRenamer {[m
 [m
         def display = edition ? "$title, $edition Ed" : title[m
 [m
[31m-        return "$display [${dpub(pub)}, $isbn, $pubDate]"[m
[32m+[m[32m        return "$display [${dpub(pub, isbn)}, $isbn, $pubDate]"[m
     }[m
 [m
[31m-    def dpub(name) {[m
[31m-        return pubmap.map( (name ?: "").toLowerCase()) ?: name[m
[32m+[m[32m    def dpub(name, isbn) {[m
[32m+[m[32m        def result = pubmap.map( (name ?: "").toLowerCase())[m
[32m+[m
[32m+[m[32m        if (!result) {[m
[32m+[m[32m            def keys = (5..11).collect { isbn[3..it] }[m
[32m+[m[32m            for(String key : keys) {[m
[32m+[m[32m                if ((result = pubmap.map(key)))[m
[32m+[m[32m                    return result[m
[32m+[m[32m            }[m
[32m+[m[32m            return name[m
[32m+[m[32m        }[m
     }[m
 [m
     def clean(name) {[m
[36m@@ -256,7 +265,10 @@[m [mclass IsbnRenamer {[m
             println("")[m
             def detail = cache.find(list[0].isbn)[m
             if (detail) {[m
[31m-                println("\t Author(s):\t\t\tPages: ${detail.pageCount}")[m
[32m+[m[32m                if (detail.kind == 'video')[m
[32m+[m[32m                    println("\t Author(s):\t\t\tLength: ${detail.runLength} minutes")[m
[32m+[m[32m                else[m
[32m+[m[32m                    println("\t Author(s):\t\t\tPages: ${detail.pageCount}")[m
                 println("\t    ${detail.authors.join(', ')}")[m
                 println("\tCategories:\n\t    ${detail.categories.join(', ')}\n")[m
             }[m
[36m@@ -308,13 +320,19 @@[m [mclass IsbnRenamer {[m
         return command.execute().text[m
     }[m
 [m
[32m+[m[32m    String annotate(String extra, String text) {[m
[32m+[m[32m        return "  $extra\n  $text"[m
[32m+[m[32m    }[m
[32m+[m
     List parse(String text) {[m
[32m+[m[32mprintln("got $text")[m
         def isbnPatterns = ~/\s((\d{9}([xX]|\d))|\d{13})\s/[m
 [m
         return text.replaceAll('-', "").findAll(isbnPatterns)*.trim()[m
     }[m
 [m
     List validate(List isbnList) {[m
[32m+[m[32mprintln("got $isbnList")[m
         return isbnList.inject([]) { r, v ->[m
             def isbn = checkIsbn(v)[m
 [m
