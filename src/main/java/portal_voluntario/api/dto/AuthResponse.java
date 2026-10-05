package portal_voluntario.api.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UsuarioResponse usuario) {
}
