package portal_voluntario.api.dto;
import java.util.UUID;
import portal_voluntario.domain.Documento;
public record DocumentoResponse(UUID id, String nome, String tipo, long tamanho, String sha256) {
    public static DocumentoResponse from(Documento d) {
        return new DocumentoResponse(d.getId(), d.getNomeOriginal(), d.getTipo(), d.getTamanho(), d.getSha256());
    }
}
