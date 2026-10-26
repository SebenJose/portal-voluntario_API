package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.BancoHorasEstudanteResponse;
import portal_voluntario.domain.StatusParticipacao;
import portal_voluntario.service.DashboardService;
import portal_voluntario.service.RelatorioService;

@Tag(name = "Relatórios", description = "Exportações privadas de participantes e saldos de horas.")
@RestController
public class RelatorioController {
    private final RelatorioService relatorios;
    private final DashboardService dashboard;

    public RelatorioController(RelatorioService relatorios, DashboardService dashboard) {
        this.relatorios = relatorios;
        this.dashboard = dashboard;
    }

    @GetMapping("/api/v1/oportunidades/{id}/inscricoes/exportacao")
    @ApiResponse(responseCode = "200", description = "CSV de participantes para download",
            content = @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")))
    @Operation(summary = "Exportar participantes em CSV",
            description = "Responsável ou ADMINISTRADOR. Combina busca por nome/e-mail e filtro de presença. Inclui atividades canceladas. CSV UTF-8 com BOM, separador ponto e vírgula e proteção de células contra fórmulas.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    public ResponseEntity<byte[]> participantes(@PathVariable Long id,
            @RequestParam(required = false) StatusParticipacao status,
            @RequestParam(required = false) String busca, Authentication auth) {
        return csv("participantes-" + id + ".csv", relatorios.participantes(id, status, busca, auth));
    }

    @GetMapping("/api/v1/admin/relatorios/banco-horas")
    @Operation(summary = "Consultar saldos de todos os estudantes",
            description = "ADMINISTRADOR. Inclui contas inativas e saldos zerados, ordenados por nome e ID. O cálculo coincide com o painel do titular e usa o teto atual.")
    public ResponseEntity<List<BancoHorasEstudanteResponse>> bancoHoras() {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(dashboard.consultarEstudantes());
    }

    @GetMapping("/api/v1/admin/relatorios/banco-horas/exportacao")
    @ApiResponse(responseCode = "200", description = "CSV de saldos por estudante/eixo para download",
            content = @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")))
    @Operation(summary = "Exportar banco de horas em CSV",
            description = "ADMINISTRADOR. Uma linha por estudante/eixo configurado; inclui contas inativas e saldos zerados. UTF-8 com BOM, ponto e vírgula, decimal com vírgula e duas casas. Sem eixos ou estudantes, retorna somente o cabeçalho.")
    public ResponseEntity<byte[]> exportarBancoHoras() {
        return csv("banco-horas.csv", relatorios.bancoHoras());
    }

    private ResponseEntity<byte[]> csv(String nome, byte[] conteudo) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox").body(conteudo);
    }
}
