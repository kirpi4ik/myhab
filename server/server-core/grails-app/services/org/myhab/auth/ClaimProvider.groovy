package org.myhab.auth

import com.nimbusds.jwt.JWTClaimsSet
import grails.plugin.springsecurity.rest.token.generation.jwt.CustomClaimProvider
import org.springframework.security.core.userdetails.UserDetails

class ClaimProvider implements CustomClaimProvider {

    @Override
    void provideCustomClaims(JWTClaimsSet.Builder builder, UserDetails details, String principal, Integer expiration) {
        // NOTE: do not set expirationTime here — the token generator already
        // sets `exp` from grails.plugin.springsecurity.rest.token.storage.jwt.expiration
        // (application.groovy). Overriding it here silently turns that config
        // into dead configuration (which is exactly what happened before).
        builder.issuer("myhab")
    }

}
