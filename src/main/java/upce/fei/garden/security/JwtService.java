package upce.fei.garden.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import upce.fei.garden.model.enums.UserRole;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Vytváří a ověřuje JWT tokeny používané pro stateless autentizaci API.
 * Token nese e-mail uživatele jako subject a jeho roli ({@link UserRole}) jako claim "role".
 * Klíč pro podpis (HMAC) se odvozuje z {@code app.jwt.secret}, platnost tokenu z {@code app.jwt.expiration}.
 */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Vygeneruje podepsaný JWT pro daného uživatele.
     */
    public String generateToken(String email, UserRole role) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    /**
     * Vrátí e-mail (subject) uloženy v tokenu. Token musí být předem ověřen pomocí {@link #isValid(String)}.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Vrátí roli uloženou v tokenu jako claim "role".
     */
    public UserRole extractRole(String token) {
        return UserRole.valueOf(parseClaims(token).get(ROLE_CLAIM, String.class));
    }

    /**
     * Ověří podpis a platnost (expiraci) tokenu. Nevyhazuje výjimku – při jakémkoliv
     * problému (neplatný podpis, expirovaný token, poškozený formát) vrací false.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
