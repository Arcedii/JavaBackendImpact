package com.impact.lessons.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret; // Secret pentru semnarea token-ului JWT, injectat din fișierul de proprietăți.

    @Value("${jwt.expiration}")
    private Long expiration; // Timpul de expirare al token-ului, injectat din fișierul de proprietăți.

    // Extrage username-ul din token-ul JWT.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extrage data de expirare din token-ul JWT.
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extrage o singură "revendicare" (claim) din token folosind un resolver.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token); // Extrage toate revendicările din token.
        return claimsResolver.apply(claims); // Aplică resolver-ul pentru a obține revendicarea specifică.
    }

    // Extrage toate revendicările din token-ul JWT.
    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody(); // Parsează și validează token-ul folosind secretul.
    }

    // Verifică dacă token-ul a expirat.
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date()); // Compară data de expirare cu data curentă.
    }

    // Generează un token JWT pentru un utilizator.
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(); // Creează o hartă goală pentru revendicări suplimentare.
        return createToken(claims, userDetails.getUsername()); // Creează token-ul cu revendicările și subiectul (username-ul).
    }

    // Creează un nou token JWT.
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims) // Setează revendicările.
                .setSubject(subject) // Setează subiectul token-ului (de obicei, username-ul).
                .setIssuedAt(new Date(System.currentTimeMillis())) // Setează data emiterii token-ului.
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Setează data de expirare.
                .signWith(SignatureAlgorithm.HS256, secret) // Semnează token-ul folosind algoritmul HS256 și secretul.
                .compact(); // Construiește și serializează token-ul într-un string.
    }

    // Validează token-ul JWT.
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token); // Extrage username-ul din token.
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token)); // Verifică dacă username-ul corespunde și token-ul nu a expirat.
    }
}
