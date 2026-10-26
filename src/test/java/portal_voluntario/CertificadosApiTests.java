package portal_voluntario;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;
import portal_voluntario.service.DashboardService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CertificadosApiTests {
    static final Path directory = criarDiretorio();
    static Path criarDiretorio() {
        try { return Files.createTempDirectory("portal-documentos-test-"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.directory", directory::toString);
    }
    @AfterAll static void cleanup() throws Exception {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired RegistroAtividadeRepository registros;
    @Autowired CertificadoEmitidoRepository certificados;
    @Autowired DocumentoRepository documentos;
    @Autowired DashboardService dashboard;
    @Autowired portal_voluntario.service.CertificadoEmailService emails;
    @MockitoBean JavaMailSender mail;
    Usuario estudante, outro, organizador, admin;
    Oportunidade curso;
    Inscricao presente, ausente;
    GrupoRegulamento eixo;
    byte[] pdfUpload = "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @BeforeEach void preparar() {
        estudante = usuarios.save(new Usuario("José da Conceição", "pessoa@certificados.test", "senha", null, Role.ESTUDANTE));
        outro = usuarios.save(new Usuario("Outra pessoa", "outra@certificados.test", "senha", null, Role.ESTUDANTE));
        organizador = usuarios.save(new Usuario("Organizador", "org@certificados.test", "senha", null, Role.ORGANIZADOR));
        admin = usuarios.save(new Usuario("Admin", "admin@certificados.test", "senha", null, Role.ADMINISTRADOR));
        eixo = grupos.save(new GrupoRegulamento(CategoriaEixo.EXTENSAO, new BigDecimal("100")));
        OffsetDateTime inicio = OffsetDateTime.now().minusDays(1);
        curso = oportunidades.save(new Oportunidade("Oficina solidária", "Descrição", new BigDecimal("2.5"), 10,
                inicio, inicio.plusHours(3), eixo, organizador));
        presente = new Inscricao(estudante, curso); presente.marcarPresenca(StatusParticipacao.PRESENTE); inscricoes.save(presente);
        ausente = new Inscricao(outro, curso); ausente.marcarPresenca(StatusParticipacao.AUSENTE); inscricoes.save(ausente);
    }
    RequestPostProcessor auth(Usuario user) {
        return jwt().jwt(value -> value.subject(user.getEmail())).authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
    void emitir() throws Exception {
        mvc.perform(post("/api/v1/oportunidades/" + curso.getId() + "/certificados/emitir").with(auth(organizador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inscricaoIds\":[" + presente.getId() + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").isNotEmpty());
    }
    UUID enviarArquivo() throws Exception {
        mvc.perform(multipart("/api/v1/documentos").file(new MockMultipartFile("arquivo", "../comprovante.pdf", "application/pdf", pdfUpload)).with(auth(estudante)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sha256").isNotEmpty());
        return documentos.findAll().getFirst().getId();
    }

    @Test void uploadEPrivadoEPermiteDownloadAoProprietarioEAdmin() throws Exception {
        UUID id = enviarArquivo();
        String route = "/api/v1/documentos/" + id;
        mvc.perform(get(route).with(auth(estudante))).andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(content().bytes(pdfUpload));
        mvc.perform(get(route).with(auth(outro))).andExpect(status().isNotFound());
        mvc.perform(get(route).with(auth(organizador))).andExpect(status().isNotFound());
        mvc.perform(get(route).with(auth(admin))).andExpect(status().isOk());
        mvc.perform(get(route)).andExpect(status().isUnauthorized());
        assertThat(Files.exists(directory.resolve(id.toString()))).isTrue();
        assertThat(documentos.findById(id).orElseThrow().getNomeOriginal()).doesNotContain("/");
    }

    @Test void uploadRejeitaFormatoTamanhoEQuota() throws Exception {
        for (MockMultipartFile file : new MockMultipartFile[] {
                new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)),
                new MockMultipartFile("arquivo", "teste.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8)),
                new MockMultipartFile("arquivo", "vazio.pdf", "application/pdf", new byte[0]),
                new MockMultipartFile("arquivo", "grande.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]) }) {
            mvc.perform(multipart("/api/v1/documentos").file(file).with(auth(estudante))).andExpect(status().isBadRequest());
        }
        assertThat(documentos.count()).isZero();
        documentos.save(new Documento(UUID.randomUUID(), estudante, "arquivo-antigo.pdf", "application/pdf", 104857600L, "0".repeat(64)));
        mvc.perform(multipart("/api/v1/documentos").file(new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", pdfUpload)).with(auth(estudante)))
                .andExpect(status().isConflict());
    }

    @Test void comprovanteDoUploadPodeSerSubmetidoSomentePeloProprietario() throws Exception {
        UUID id = enviarArquivo();
        String body = "{\"titulo\":\"Atividade externa\",\"horasDeclaradas\":3,\"grupoHorasId\":" + eixo.getId() + ",\"documentoId\":\"" + id + "\"}";
        mvc.perform(post("/api/v1/submissoes-externas").with(auth(estudante)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.documentoId").value(id.toString()))
                .andExpect(jsonPath("$.urlDocumento").isEmpty());
        mvc.perform(post("/api/v1/submissoes-externas").with(auth(outro)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        String ambos = body.substring(0, body.length() - 1) + ",\"urlDocumento\":\"https://example.org/doc\"}";
        mvc.perform(post("/api/v1/submissoes-externas").with(auth(estudante)).contentType(MediaType.APPLICATION_JSON).content(ambos))
                .andExpect(status().isBadRequest());
    }

    @Test void documentoAlteradoNoDiscoNaoEPossivelBaixar() throws Exception {
        UUID id = enviarArquivo();
        Files.writeString(directory.resolve(id.toString()), "corrompido");
        mvc.perform(get("/api/v1/documentos/" + id).with(auth(estudante))).andExpect(status().isConflict());
    }

    @Test void arquivoNaoUtilizadoPodeSerRemovidoMasComprovanteSubmetidoEPreservado() throws Exception {
        UUID primeiro = enviarArquivo();
        mvc.perform(delete("/api/v1/documentos/" + primeiro).with(auth(outro))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/documentos/" + primeiro).with(auth(estudante))).andExpect(status().isNoContent());
        assertThat(documentos.findById(primeiro)).isEmpty();
        UUID segundo = enviarArquivo();
        mvc.perform(post("/api/v1/submissoes-externas").with(auth(estudante)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Atividade externa\",\"horasDeclaradas\":3,\"grupoHorasId\":" + eixo.getId() + ",\"documentoId\":\"" + segundo + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/documentos/" + segundo).with(auth(estudante))).andExpect(status().isConflict());
        assertThat(documentos.findById(segundo)).isPresent();
    }

    @Test void pdfRealTemNomeAcentosCodigoEVerificacaoPublicaSemDadosPrivados() throws Exception {
        emitir();
        UUID codigo = certificados.findAll().getFirst().getCodigo();
        byte[] bytes = mvc.perform(get("/api/v1/certificados/" + codigo + "/pdf").with(auth(estudante)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("José da Conceição", "Oficina solidária", codigo.toString());
        }
        mvc.perform(get("/api/v1/certificados/verificar/" + codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("VALIDO"))
                .andExpect(jsonPath("$.beneficiario").value("José da Conceição"))
                .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.matriculaRa").doesNotExist());
        mvc.perform(get("/api/v1/certificados/" + codigo + "/pdf").with(auth(outro))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/certificados/" + codigo + "/pdf")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/certificados/meus").with(auth(estudante))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(codigo.toString()));
        mvc.perform(get("/api/v1/certificados/meus").with(auth(outro))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void reemissaoNaoDuplicaHorasOuCodigoESnapshotPreservaDados() throws Exception {
        emitir();
        UUID codigo = certificados.findAll().getFirst().getCodigo();
        curso.atualizar("Nome alterado", "Descrição", new BigDecimal("8"), 10, curso.getDataInicio(), curso.getDataFim(), eixo);
        emitir();
        assertThat(registros.count()).isEqualTo(1); assertThat(certificados.count()).isEqualTo(1);
        assertThat(certificados.findAll().getFirst().getCodigo()).isEqualTo(codigo);
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("2.5");
        mvc.perform(get("/api/v1/certificados/verificar/" + codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.atividade").value("Oficina solidária")).andExpect(jsonPath("$.horas").value(2.5));
    }

    @Test void emissaoExigeGestorEncerramentoEPresencaEmTodoLote() throws Exception {
        String route = "/api/v1/oportunidades/" + curso.getId() + "/certificados/emitir";
        String body = "{\"inscricaoIds\":[" + presente.getId() + "," + ausente.getId() + "]}";
        mvc.perform(post(route).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(post(route).with(auth(estudante)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/oportunidades/" + curso.getId() + "/certificados")).andExpect(status().isUnauthorized());
        Usuario outroOrganizador = usuarios.save(new Usuario("Outro org", "outro-org@certificados.test", "senha", null, Role.ORGANIZADOR));
        mvc.perform(post(route).with(auth(outroOrganizador)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        curso.atualizar(curso.getTitulo(), curso.getDescricao(), curso.getCargaHoraria(), 10,
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2), eixo);
        mvc.perform(post(route).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inscricaoIds\":[" + presente.getId() + "]}")).andExpect(status().isConflict());
        assertThat(registros.count()).isZero(); verifyNoInteractions(mail);
    }

    @Test void revogacaoRetiraHorasInvalidaCodigoEBloqueiaReemissao() throws Exception {
        emitir();
        var certificado = certificados.findAll().getFirst();
        long registroId = certificado.getRegistro().getId();
        String route = "/api/v1/oportunidades/" + curso.getId() + "/certificados/" + registroId + "/revogar";
        mvc.perform(post(route).with(auth(estudante)).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Correção\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(route).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Correção de presença\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("REVOGADO"));
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("0");
        emails.enviarPendentes(); verifyNoInteractions(mail);
        mvc.perform(get("/api/v1/certificados/verificar/" + certificado.getCodigo())).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REVOGADO"));
        mvc.perform(get("/api/v1/certificados/" + certificado.getCodigo() + "/pdf").with(auth(estudante))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/oportunidades/" + curso.getId() + "/certificados/emitir").with(auth(organizador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inscricaoIds\":[" + presente.getId() + "]}")).andExpect(status().isConflict());
        assertThat(certificados.count()).isEqualTo(1);
    }

    @Test void falhaSmtpPreservaPdfEHorasESituacaoDoEnvio() throws Exception {
        doThrow(new MailSendException("SMTP indisponível")).when(mail).send(any(SimpleMailMessage.class));
        emitir();
        emails.enviarPendentes();
        var certificado = certificados.findAll().getFirst();
        assertThat(certificado.getRegistro().getStatusEnvioEmail()).isEqualTo(StatusEnvioEmail.FALHOU);
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("2.5");
        mvc.perform(get("/api/v1/certificados/" + certificado.getCodigo() + "/pdf").with(auth(estudante))).andExpect(status().isOk());
    }

    @Test void emissaoEnfileiraAvisosEProcessamentoNaoRepeteEnviados() throws Exception {
        emitir();
        verifyNoInteractions(mail);
        assertThat(registros.findAll().getFirst().getStatusEnvioEmail()).isEqualTo(StatusEnvioEmail.PENDENTE);
        emails.enviarPendentes();
        assertThat(registros.findAll().getFirst().getStatusEnvioEmail()).isEqualTo(StatusEnvioEmail.ENVIADO);
        emails.enviarPendentes();
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }
}
