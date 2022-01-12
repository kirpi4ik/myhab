package org.myhab.auth

import com.nimbusds.jwt.JWTClaimsSet
import grails.plugin.springsecurity.rest.token.generation.jwt.CustomClaimProvider
import org.joda.time.DateTime
import org.springframework.security.core.userdetails.UserDetails

class ClaimProvider implements CustomClaimProvider {

    @Override
    void provideCustomClaims(JWTClaimsSet.Builder builder, UserDetails details, String principal, Integer expiration) {
        builder.expirationTime(DateTime.now().plusMinutes(300000).toDate())
        builder.issuer("myhab")
    }

}