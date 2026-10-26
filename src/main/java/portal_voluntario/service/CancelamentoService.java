package portal_voluntario.service;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.AvisosCancelamentoResponse;
import portal_voluntario.api.dto.OportunidadeResponse;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;

@Service
public class CancelamentoService {
    private final OportunidadeRepository oportunidades;
    private final InscricaoRepository inscricoes;
    private final UsuarioRepository usuarios;

    public CancelamentoService(OportunidadeRepository oportunidades, InscricaoRepository inscricoes, UsuarioRepository usuarios) {
        this.oportunidades = oportunidades;
        this.inscricoes = inscricoes;
        this.usuarios = usuarios;
    }

    @Transactional
    public OportunidadeResponse cancelar(Long id, String motivo, String email) {
        Oportunidade oportunidade = oportunidades.findByIdForUpdate(id).orElseThrow(this::naoEncontrada);
        Usuario responsavel = validarGestor(oportunidade, email);
        if (oportunidade.isCancelada()) throw conflito("Esta atividade já foi cancelada");
        if (!oportunidade.getDataInicio().isAfter(OffsetDateTime.now())) {
            throw conflito("Só é possível cancelar atividades que ainda não começaram");
        }
        oportunidade.cancelar(motivo.trim(), responsavel);
        inscricoes.findAllByOportunidadeIdOrderByDataInscricaoAsc(id)
                .forEach(inscricao -> inscricao.marcarAvisoCancelamento(StatusEnvioEmail.PENDENTE));
        return OportunidadeResponse.from(oportunidade, inscricoes.countByOportunidadeId(id));
    }

    @Transactional(readOnly = true)
    public AvisosCancelamentoResponse avisos(Long id, String email) {
        Oportunidade oportunidade = oportunidades.findById(id).orElseThrow(this::naoEncontrada);
        validarGestor(oportunidade, email);
        return resumo(id);
    }

    @Transactional
    public AvisosCancelamentoResponse reenviarFalhos(Long id, String email) {
        Oportunidade oportunidade = oportunidades.findByIdForUpdate(id).orElseThrow(this::naoEncontrada);
        validarGestor(oportunidade, email);
        if (!oportunidade.isCancelada()) throw conflito("Esta atividade não está cancelada");
        inscricoes.findAllByOportunidadeIdOrderByDataInscricaoAsc(id).stream()
                .filter(inscricao -> inscricao.getStatusAvisoCancelamento() == StatusEnvioEmail.FALHOU)
                .forEach(inscricao -> inscricao.marcarAvisoCancelamento(StatusEnvioEmail.PENDENTE));
        return resumo(id);
    }

    private AvisosCancelamentoResponse resumo(Long id) {
        return new AvisosCancelamentoResponse(
                inscricoes.countByOportunidadeIdAndStatusAvisoCancelamento(id, StatusEnvioEmail.PENDENTE),
                inscricoes.countByOportunidadeIdAndStatusAvisoCancelamento(id, StatusEnvioEmail.ENVIADO),
                inscricoes.countByOportunidadeIdAndStatusAvisoCancelamento(id, StatusEnvioEmail.FALHOU));
    }

    private Usuario validarGestor(Oportunidade oportunidade, String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(this::naoEncontrada);
        if (usuario.getRole() != Role.ADMINISTRADOR
                && (usuario.getRole() != Role.ORGANIZADOR || !oportunidade.getOrganizador().getId().equals(usuario.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não organiza esta oportunidade");
        }
        return usuario;
    }
    private ResponseStatusException naoEncontrada() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"); }
    private ResponseStatusException conflito(String motivo) { return new ResponseStatusException(HttpStatus.CONFLICT, motivo); }
}
