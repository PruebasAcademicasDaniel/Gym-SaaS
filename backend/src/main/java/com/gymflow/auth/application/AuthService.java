package com.gymflow.auth.application;

import com.gymflow.audit.application.AuditService;
import com.gymflow.audit.domain.AuditAction;
import com.gymflow.auth.domain.RefreshToken;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.RefreshTokenRepository;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.security.JwtProperties;
import com.gymflow.auth.infrastructure.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de autenticación: login, rotación de refresh token, logout.
 * El refresh token que ve el cliente es un valor opaco aleatorio; en la
 * base solo se guarda su hash SHA-256 (ver RefreshToken).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.auditService = auditService;
    }

    @Transactional
    public TokenPair login(String rawEmail, String rawPassword) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.isEnabled() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            auditService.record(
                    user != null ? user.getGymId() : null,
                    user != null ? user.getId() : null,
                    AuditAction.LOGIN_FAILURE,
                    email);
            throw new InvalidCredentialsException();
        }

        auditService.record(user.getGymId(), user.getId(), AuditAction.LOGIN_SUCCESS, null);
        return issueTokenPair(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(t -> t.isUsable(Instant.now()))
                .orElseThrow(InvalidRefreshTokenException::new);

        token.revoke();
        refreshTokenRepository.save(token);

        return issueTokenPair(token.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
            auditService.record(token.getUser().getGymId(), token.getUser().getId(), AuditAction.LOGOUT, null);
        });
    }

    private TokenPair issueTokenPair(User user) {
        String accessToken = jwtService.issueAccessToken(user);
        String rawRefreshToken = generateOpaqueToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenTtl());

        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), expiresAt));

        return new TokenPair(accessToken, rawRefreshToken, jwtProperties.getAccessTokenTtl().toSeconds());
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
