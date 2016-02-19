
package org.miner.utils

import org.yaml.snakeyaml.Yaml

class ConfigFile {
    def values

    ConfigFile(String path) {
        values = new Yaml().load(new File(path).text)

        fixup(values, null)
    }

    def fixup(Map c, Map p) {
        c.each { k, v ->
            c[k] = fixup(v, c)
        }
        return c
    }

    def fixup(List c, Map p) {
        return c.collect { fixup(it, p) }
    }

    def fixup(String s, Map p) {
        def variables = ~/\$(\{([^}]+)\}|([A-Za-z0-9_]+))/
        s.replaceAll(variables) {
            (it[2] ?: it[3]).with {
                System.properties[it] ?: System.getenv(it) ?: p[it]
            }
        }
    }

    def fixup(Object o, Map p) {
        return o
    }
}
