
package org.miner.application.isbn

import groovy.json.JsonSlurper

class GoogleBooks {
    GoogleBooks(args) {
        api.base = args.api
        trackingPath = args.trackingPath
        delay = args.delay

        api.isbnFetch = { isbn ->
            return new URL(api.base + '/volumes?q=isbn:' + isbn)
        }
    }

    long lastScheduledTime() {
        try {
            return Long.parseLong(new File(trackingPath).text)
        } catch (java.io.FileNotFoundException e) {
            return now()
        }
    }

    long lastScheduledTime(long value) {
        new File(trackingPath).text = value
        return value
    }

    def now() {
        return new Date().time
    }

    def isbnFetch(code) {
        def span = Math.round((lastScheduledTime() - now()) / 1000)

        if (span > 0) {
            (span..1).each {
                notice(String.format("\r\tWaiting %02d seconds  ", it))
                System.sleep(1000)
            }
            notice("\r\t                      \r")
        }

        try {
            return new JsonSlurper().parseText(api.isbnFetch(code).text)
        } finally {
            lastScheduledTime(now() + delay)
        }
        return ""  // failed
    }

    def notice(args) {
        print(args)
        System.out.flush()
    }

    def api = [:]
    def trackingPath, delay
}
