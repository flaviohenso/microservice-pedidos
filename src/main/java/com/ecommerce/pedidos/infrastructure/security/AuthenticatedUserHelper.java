package com.ecommerce.pedidos.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utilitário para extrair informações do usuário/cliente autenticado a partir do token JWT.
 */
@Component
public class AuthenticatedUserHelper {

    /**
     * Retorna o ID do cliente autenticado presente no token JWT.
     * Suporta claims customizados "cliente_id", "clienteId" ou o subject padrão "sub".
     */
    public Optional<Long> getAuthenticatedClienteId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        // 1. Tenta obter claim específico 'cliente_id' ou 'clienteId'
        Object clienteIdClaim = jwt.getClaims().get("cliente_id");
        if (clienteIdClaim == null) {
            clienteIdClaim = jwt.getClaims().get("clienteId");
        }

        if (clienteIdClaim instanceof Number num) {
            return Optional.of(num.longValue());
        } else if (clienteIdClaim instanceof String str) {
            try {
                return Optional.of(Long.parseLong(str));
            } catch (NumberFormatException ignored) {}
        }

        // 2. Se o subject for numérico (ID do usuário)
        String subject = jwt.getSubject();
        if (subject != null) {
            try {
                return Optional.of(Long.parseLong(subject));
            } catch (NumberFormatException ignored) {}
        }

        return Optional.empty();
    }

    /**
     * Verifica se o usuário autenticado possui role de ADMIN
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }
}
