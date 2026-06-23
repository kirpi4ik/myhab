package org.myhab.services.voice

import kong.unirest.Unirest
import kong.unirest.UnirestInstance

/**
 * A dedicated Unirest instance for LLM calls, isolated from the global Unirest
 * singleton.
 *
 * <p>{@code ConfigProvider} pins the global Unirest config to a 2-second socket
 * timeout (right for fast Gitea config pings, far too short for LLM inference,
 * which routinely takes several seconds). Spawning a separate instance gives the
 * voice providers their own connection pool and generous timeouts without
 * touching — or being reset by — that global config.</p>
 */
class VoiceHttp {

    static final UnirestInstance INSTANCE = build()

    private static UnirestInstance build() {
        UnirestInstance instance = Unirest.spawnInstance()
        // Configure once, before first use (Unirest forbids reconfiguring a live client).
        instance.config()
                .connectTimeout(5000)
                .socketTimeout(60000)
        return instance
    }
}
