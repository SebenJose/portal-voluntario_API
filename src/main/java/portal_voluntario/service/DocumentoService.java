package portal_voluntario.service;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;

@Service
public class DocumentoService {
    public static final long MAX_BYTES = 5 * 1024 * 1024;
    private final Path directory;
    private final long maxUserBytes;
    private final DocumentoRepository documentos;
    private final UsuarioRepository usuarios;
    private final SubmissaoExternaRepository submissoes;

    public DocumentoService(DocumentoRepository documentos, UsuarioRepository usuarios, SubmissaoExternaRepository submissoes,
            @Value("${app.storage.directory}") String directory, @Value("${app.storage.max-user-bytes}") long maxUserBytes) {
        this.documentos = documentos; this.usuarios = usuarios;
        this.submissoes = submissoes;
        this.directory = Path.of(directory).toAbsolutePath().normalize(); this.maxUserBytes = maxUserBytes;
    }

    @Transactional
    public Documento salvar(MultipartFile arquivo, String email) {
        if (arquivo.isEmpty() || arquivo.getSize() > MAX_BYTES) throw erro(HttpStatus.BAD_REQUEST, "Envie um arquivo de até 5 MB");
        Usuario usuario = usuarios.findByEmailForUpdate(email).orElseThrow(() -> erro(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (documentos.totalBytesPorUsuario(usuario.getId()) + arquivo.getSize() > maxUserBytes) {
            throw erro(HttpStatus.CONFLICT, "O limite de armazenamento da conta foi atingido");
        }
        try {
            byte[] bytes = arquivo.getBytes();
            String tipo = identificar(bytes);
            if (!tipo.equals(arquivo.getContentType())) throw erro(HttpStatus.BAD_REQUEST, "O conteúdo do arquivo não corresponde ao tipo informado");
            UUID id = UUID.randomUUID();
            Path target = directory.resolve(id.toString());
            Files.createDirectories(directory);
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                output.write(bytes);
            } catch (IOException exception) { Files.deleteIfExists(target); throw exception; }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try { Files.deleteIfExists(target); } catch (IOException ignored) { }
                    }
                }
            });
            String nome = Optional.ofNullable(arquivo.getOriginalFilename()).orElse("comprovante")
                    .replaceAll("[\\p{Cntrl}/\\\\]", "_");
            if (nome.isBlank()) nome = "comprovante";
            nome = nome.substring(0, Math.min(180, nome.length()));
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return documentos.saveAndFlush(new Documento(id, usuario, nome, tipo, bytes.length, hash));
        } catch (IOException exception) { throw erro(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível armazenar o arquivo"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    @Transactional(readOnly = true)
    public Download baixar(UUID id, String email) {
        Documento documento = documentos.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(() -> erro(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (!documento.getUsuario().getId().equals(usuario.getId()) && usuario.getRole() != Role.ADMINISTRADOR) {
            throw erro(HttpStatus.NOT_FOUND, "Documento não encontrado");
        }
        try {
            Path path = directory.resolve(documento.getId().toString());
            if (Files.isSymbolicLink(path)) throw erro(HttpStatus.NOT_FOUND, "Documento não encontrado");
            if (Files.size(path) != documento.getTamanho()) throw erro(HttpStatus.CONFLICT, "Não foi possível verificar a integridade do arquivo");
            byte[] bytes = Files.readAllBytes(path);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(documento.getSha256())) throw erro(HttpStatus.CONFLICT, "Não foi possível verificar a integridade do arquivo");
            return new Download(documento.getNomeOriginal(), documento.getTipo(), bytes);
        } catch (NoSuchFileException exception) { throw erro(HttpStatus.NOT_FOUND, "Documento não encontrado"); }
        catch (IOException exception) { throw erro(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível ler o arquivo"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private String identificar(byte[] bytes) {
        if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') return "application/pdf";
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[] {(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (bytes.length >= 3 && bytes[0] == (byte)255 && bytes[1] == (byte)216 && bytes[2] == (byte)255) return "image/jpeg";
        throw erro(HttpStatus.BAD_REQUEST, "Envie um documento PDF ou uma imagem PNG/JPEG");
    }

    @Transactional
    public void remover(UUID id, String email) {
        Documento documento = documentos.findByIdForUpdate(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(() -> erro(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (!documento.getUsuario().getId().equals(usuario.getId()) && usuario.getRole() != Role.ADMINISTRADOR) {
            throw erro(HttpStatus.NOT_FOUND, "Documento não encontrado");
        }
        if (submissoes.existsByDocumentoId(id)) throw erro(HttpStatus.CONFLICT, "O arquivo já faz parte de uma submissão e deve ser preservado");
        documentos.delete(documento);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { Files.deleteIfExists(directory.resolve(id.toString())); }
                catch (IOException exception) { org.slf4j.LoggerFactory.getLogger(DocumentoService.class).warn("Falha na limpeza de arquivo removido; verifique o armazenamento."); }
            }
        });
    }
    private ResponseStatusException erro(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    public record Download(String nome, String tipo, byte[] bytes) { }
}
