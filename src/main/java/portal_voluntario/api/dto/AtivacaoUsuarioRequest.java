package portal_voluntario.api.dto;
import jakarta.validation.constraints.NotNull;
public record AtivacaoUsuarioRequest(@NotNull Boolean ativo) { }
