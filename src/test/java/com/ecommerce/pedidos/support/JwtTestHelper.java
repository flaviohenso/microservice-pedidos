package com.ecommerce.pedidos.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Utilitário de testes para gerar tokens JWT válidos assinados com a chave de teste/desenvolvimento.
 */
@TestComponent
public class JwtTestHelper {

    private final String secretKey;
    private final JwtEncoder encoder;

    public JwtTestHelper(@Value("${security.jwt.secret-key}") String secretKey) {
        this.secretKey = secretKey;
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(keySpec));
    }

    /**
     * Gera um token JWT com subject e roles especificadas
     */
    public String gerarToken(Long clienteId, String... roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("microservice-auth")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(String.valueOf(clienteId))
                .claim("cliente_id", clienteId)
                .claim("roles", List.of(roles))
                .build();

        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        Jwt jwt = encoder.encode(JwtEncoderParameters.from(header, claims));
        return jwt.getTokenValue();
    }
}
