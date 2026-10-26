package portal_voluntario.service;

import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.*;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;

@Service
public class CertificadoService {
    private final CertificadoEmitidoRepository certificados;
    private final RegistroAtividadeRepository registros;
    private final OportunidadeRepository oportunidades;
    private final InscricaoRepository inscricoes;
    private final UsuarioRepository usuarios;
    private final GrupoRegulamentoRepository grupos;
    private final CertificadoPdfService pdf;
    private final String frontend;

    public CertificadoService(CertificadoEmitidoRepository certificados, RegistroAtividadeRepository registros,
            OportunidadeRepository oportunidades, InscricaoRepository inscricoes, UsuarioRepository usuarios,
            CertificadoPdfService pdf, GrupoRegulamentoRepository grupos, @Value("${app.frontend-url}") String frontend) {
        this.certificados = certificados; this.registros = registros; this.oportunidades = oportunidades;
        this.inscricoes = inscricoes; this.usuarios = usuarios; this.pdf = pdf;
        this.frontend = frontend.replaceAll("/+$", "");
        this.grupos = grupos;
    }

    @Transactional
    public List<CertificadoGestaoResponse> emitir(Long oportunidadeId, EmitirCertificadosRequest request, String email) {
        Oportunidade oportunidade = oportunidades.findByIdForUpdate(oportunidadeId).orElseThrow(this::naoEncontrado);
        validarGestor(oportunidade, email);
        if (oportunidade.isCancelada()) throw erro(HttpStatus.CONFLICT, "Esta atividade foi cancelada");
        if (oportunidade.getDataFim().isAfter(OffsetDateTime.now())) throw erro(HttpStatus.CONFLICT, "Aguarde o encerramento da atividade para emitir certificados");
        Set<Long> ids = new HashSet<>(request.inscricaoIds());
        if (ids.size() != request.inscricaoIds().size()) throw erro(HttpStatus.BAD_REQUEST, "Não repita destinatários");
        List<Inscricao> participantes = inscricoes.findAllById(ids);
        if (participantes.size() != ids.size() || participantes.stream().anyMatch(item ->
                !item.getOportunidade().getId().equals(oportunidadeId) || item.getStatusParticipacao() != StatusParticipacao.PRESENTE)) {
            throw erro(HttpStatus.BAD_REQUEST, "Os destinatários devem estar presentes e inscritos nesta atividade");
        }
        // Validate the whole batch before sending any messages.
        for (Inscricao inscricao : participantes) {
            if (registros.findByUsuarioIdAndOportunidadeId(inscricao.getUsuario().getId(), oportunidadeId).filter(RegistroAtividade::isRevogado).isPresent()) {
                throw erro(HttpStatus.CONFLICT, "Há um registro revogado entre os destinatários; ele não pode ser reemitido");
            }
        }
        List<CertificadoGestaoResponse> result = new ArrayList<>();
        for (Inscricao inscricao : participantes) {
            RegistroAtividade registro = registros.findByUsuarioAndOportunidadeForUpdate(inscricao.getUsuario().getId(), oportunidadeId)
                    .orElseGet(() -> registros.save(new RegistroAtividade(inscricao.getUsuario(), oportunidade, oportunidade.getCargaHoraria(), null)));
            CertificadoEmitido certificado = certificados.findByRegistroId(registro.getId())
                    .orElseGet(() -> certificados.save(new CertificadoEmitido(registro)));
            String link = frontend + "/verificar-certificado/" + certificado.getCodigo();
            registro.atualizarCertificado(link);
            result.add(CertificadoGestaoResponse.from(registro, certificado));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CertificadoPessoalResponse> meus(String email) {
        return registros.findAllByUsuarioEmailIgnoreCase(email).stream().map(registro ->
                CertificadoPessoalResponse.from(registro, certificados.findByRegistroId(registro.getId()).orElse(null))).toList();
    }

    @Transactional(readOnly = true)
    public List<CertificadoGestaoResponse> gerenciar(Long oportunidadeId, String email) {
        Oportunidade oportunidade = oportunidades.findById(oportunidadeId).orElseThrow(this::naoEncontrado);
        validarGestor(oportunidade, email);
        return registros.findAllByOportunidadeIdOrderByIdDesc(oportunidadeId).stream().map(registro ->
                CertificadoGestaoResponse.from(registro, certificados.findByRegistroId(registro.getId()).orElse(null))).toList();
    }

    @Transactional
    public CertificadoGestaoResponse revogar(Long oportunidadeId, Long registroId, String motivo, String email) {
        Oportunidade oportunidade = oportunidades.findByIdForUpdate(oportunidadeId).orElseThrow(this::naoEncontrado);
        validarGestor(oportunidade, email);
        RegistroAtividade registro = registros.findByIdForUpdate(registroId)
                .filter(item -> item.getOportunidade().getId().equals(oportunidadeId)).orElseThrow(this::naoEncontrado);
        registro.revogar(motivo.trim());
        return CertificadoGestaoResponse.from(registro, certificados.findByRegistroId(registroId).orElse(null));
    }

    @Transactional(readOnly = true)
    public VerificacaoCertificadoResponse verificar(UUID codigo) {
        return VerificacaoCertificadoResponse.from(certificados.findById(codigo).orElseThrow(this::naoEncontrado));
    }

    @Transactional
    public CertificadoGestaoResponse retificar(Long oportunidadeId, Long registroId, RetificarCertificadoRequest request, String email) {
        Oportunidade oportunidade = oportunidades.findByIdForUpdate(oportunidadeId).orElseThrow(this::naoEncontrado);
        Usuario responsavel = validarGestor(oportunidade, email);
        RegistroAtividade registro = registros.findByIdForUpdate(registroId)
                .filter(item -> item.getOportunidade().getId().equals(oportunidadeId)).orElseThrow(this::naoEncontrado);
        CertificadoEmitido anterior = certificados.findByRegistroId(registroId).orElseThrow(this::naoEncontrado);
        if (!anterior.getCodigo().equals(request.codigoEsperado())) {
            throw erro(HttpStatus.CONFLICT, "O certificado foi alterado por outra sessão. Atualize os dados antes de retificar");
        }
        if (registro.isRevogado()) throw erro(HttpStatus.CONFLICT, "Certificados revogados não podem ser retificados");
        if (oportunidade.isCancelada() || oportunidade.getDataFim().isAfter(OffsetDateTime.now())) {
            throw erro(HttpStatus.CONFLICT, "A retificação exige uma atividade encerrada e não cancelada");
        }
        if (!request.dataFim().isAfter(request.dataInicio()) || request.dataFim().isAfter(OffsetDateTime.now())) {
            throw erro(HttpStatus.BAD_REQUEST, "O período corrigido deve ter término posterior ao início e não pode estar no futuro");
        }
        GrupoRegulamento grupo = grupos.findByCategoriaEixo(request.categoriaEixo())
                .orElseThrow(() -> erro(HttpStatus.BAD_REQUEST, "O eixo informado ainda não foi configurado"));
        String beneficiario = request.beneficiario().trim();
        String atividade = request.atividade().trim();
        String organizador = request.organizador().trim();
        if (anterior.getBeneficiario().equals(beneficiario) && anterior.getAtividade().equals(atividade)
                && anterior.getOrganizador().equals(organizador) && anterior.getHoras().compareTo(request.horas()) == 0
                && anterior.getCategoriaEixo() == request.categoriaEixo()
                && anterior.getDataInicio().isEqual(request.dataInicio()) && anterior.getDataFim().isEqual(request.dataFim())) {
            throw erro(HttpStatus.CONFLICT, "Altere ao menos um dado do certificado para retificar");
        }
        CertificadoEmitido novo = certificados.save(new CertificadoEmitido(anterior, beneficiario, atividade, organizador,
                request.horas(), request.categoriaEixo(), request.dataInicio(), request.dataFim(), request.motivo().trim(), responsavel));
        anterior.substituirPor(novo.getCodigo());
        registro.retificarHoras(request.horas(), grupo);
        registro.atualizarCertificado(frontend + "/verificar-certificado/" + novo.getCodigo());
        certificados.flush();
        return CertificadoGestaoResponse.from(registro, novo);
    }

    @Transactional(readOnly = true)
    public List<HistoricoCertificadoResponse> historico(UUID codigo, String email) {
        CertificadoEmitido certificado = certificados.findById(codigo).orElseThrow(this::naoEncontrado);
        validarAcesso(certificado.getRegistro(), email);
        return certificados.findAllByRegistroIdOrderByVersaoDesc(certificado.getRegistro().getId()).stream()
                .map(HistoricoCertificadoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public byte[] baixar(UUID codigo, String email) {
        CertificadoEmitido certificado = certificados.findById(codigo).orElseThrow(this::naoEncontrado);
        RegistroAtividade registro = certificado.getRegistro();
        validarAcesso(registro, email);
        if (certificado.getCodigoSubstituto() != null) throw erro(HttpStatus.CONFLICT, "Este certificado foi retificado. Consulte a emissão substituta");
        if (registro.isRevogado()) throw erro(HttpStatus.CONFLICT, "Este certificado foi revogado");
        try { return pdf.gerar(certificado, frontend + "/verificar-certificado/" + certificado.getCodigo()); }
        catch (java.io.IOException | IllegalArgumentException exception) { throw erro(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível gerar o PDF do certificado"); }
    }

    private void validarAcesso(RegistroAtividade registro, String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(this::naoEncontrado);
        if (!registro.getUsuario().getId().equals(usuario.getId()) && usuario.getRole() != Role.ADMINISTRADOR
                && !registro.getOportunidade().getOrganizador().getId().equals(usuario.getId())) throw naoEncontrado();
    }

    private Usuario validarGestor(Oportunidade oportunidade, String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(this::naoEncontrado);
        if (usuario.getRole() != Role.ADMINISTRADOR && !oportunidade.getOrganizador().getId().equals(usuario.getId())) {
            throw erro(HttpStatus.FORBIDDEN, "Você não organiza esta oportunidade");
        }
        return usuario;
    }
    private ResponseStatusException naoEncontrado() { return erro(HttpStatus.NOT_FOUND, "Certificado ou atividade não encontrado"); }
    private ResponseStatusException erro(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
