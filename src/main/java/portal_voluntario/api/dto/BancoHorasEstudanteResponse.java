package portal_voluntario.api.dto;

public record BancoHorasEstudanteResponse(
        Long usuarioId, String nome, String email, String matriculaRa,
        boolean ativo, BancoHorasResponse bancoHoras) {
}
