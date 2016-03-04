
package org.miner.application.isbn

class DataScrubber {
    def suffix = ~/(\s|[,.-])*$/
    def eds = [
        '2nd': ~/(?i)\(?(2|2nd|2th|second|two)\s*ed(ition)?\)?/
      , '3rd': ~/(?i)\(?(3|3rd|3th|third|three)\s*ed(ition)?\)?/
      , '4th': ~/(?i)\(?(4|4rd|4th|fourth|four)\s*ed(ition)?\)?/
      , '5th': ~/(?i)\(?(5|5rd|5th|fifth|five)\s*ed(ition)?\)?/
    ]

    DataScrubber(pubmap) {
        this.pubmap = pubmap
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

    def pubmap
}
