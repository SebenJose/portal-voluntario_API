package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import portal_voluntario.api.dto.DocumentoResponse;
import portal_voluntario.service.DocumentoService;

@Tag(name = "Documentos", description = "Upload e download privado de comprovantes.")
@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoController {
    private final DocumentoService service;
    public DocumentoController(DocumentoService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar comprovante",
            description = "ESTUDANTE. Campo multipart arquivo: PDF, PNG ou JPEG de até 5 MB. O conteúdo é validado e a cota padrão por usuário é 100 MB.")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "413", ref = "#/components/responses/LimiteUpload")
    @ApiResponse(responseCode = "201", description = "Operação concluída")
    public DocumentoResponse enviar(@RequestParam("arquivo") MultipartFile arquivo, Authentication auth) {
        return DocumentoResponse.from(service.salvar(arquivo, auth.getName()));
    }
    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Arquivo privado no formato original", content = {
            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary"))})
    @Operation(summary = "Baixar comprovante privado",
            description = "Titular ou ADMINISTRADOR. Retorna PDF, PNG ou JPEG conforme o arquivo armazenado. Outro usuário recebe 404.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    public ResponseEntity<byte[]> baixar(@PathVariable UUID id, Authentication auth) {
        var file = service.baixar(id, auth.getName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.nome(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store").header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox").body(file.bytes());
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover comprovante não utilizado",
            description = "Titular ou ADMINISTRADOR. Documentos já associados a uma submissão são preservados (409).")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "204", description = "Operação concluída")
    public void remover(@PathVariable UUID id, Authentication auth) { service.remover(id, auth.getName()); }
}
