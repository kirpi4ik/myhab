package org.myhab.init.cache

enum CacheMap {
    EXPIRE("expiring"),
    TOKENS("tokens")

    String name

    CacheMap(def name) {
        this.name = name
    }
}