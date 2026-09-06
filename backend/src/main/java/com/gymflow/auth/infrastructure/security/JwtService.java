package com.gymflow.auth.infrastructure.security;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Emite y valida los access tokens (JWT firmados HS256). El refresh token es un asunto aparte — ver AuthService. */
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_GYM_ID = "gymId";
    private static final String CLAIM_MEMBER_ID = "memberId";

    private final Key signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenTtl())))
                .signWith(signingKey);
        if (user.getGymId() != null) {
            builder.claim(CLAIM_GYM_ID, user.getGymId().toString());
        }
        if (user.getMemberId() != null) {
            builder.claim(CLAIM_MEMBER_ID, user.getMemberId().toString());
        }
        return builder.compact();
    }

    /** @throws JwtException si el token es inválido, está mal firmado o venció. */
    public AuthenticatedPrincipal parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get(CLAIM_EMAIL, String.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        String gymIdClaim = claims.get(CLAIM_GYM_ID, String.class);
        UUID gymId = gymIdClaim != null ? UUID.fromString(gymIdClaim) : null;
        String memberIdClaim = claims.get(CLAIM_MEMBER_ID, String.class);
        UUID memberId = memberIdClaim != null ? UUID.fromString(memberIdClaim) : null;

        return new AuthenticatedPrincipal(userId, email, role, gymId, memberId);
    }
}
