package portal_voluntario.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.api.dto.GrupoRegulamentoResponse;
import portal_voluntario.repository.GrupoRegulamentoRepository;
import portal_voluntario.domain.CategoriaEixo;
import portal_voluntario.domain.GrupoRegulamento;
import portal_voluntario.api.dto.LimiteRegulamentoRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GrupoRegulamentoService {

    private final GrupoRegulamentoRepository grupoRepository;

    public GrupoRegulamentoService(GrupoRegulamentoRepository grupoRepository) {
        this.grupoRepository = grupoRepository;
    }

    @Transactional(readOnly = true)
    public List<GrupoRegulamentoResponse> listar() {
        return grupoRepository.findAll().stream().map(GrupoRegulamentoResponse::from).toList();
    }

    @Transactional
    public GrupoRegulamentoResponse configurar(CategoriaEixo categoria, LimiteRegulamentoRequest request) {
        GrupoRegulamento grupo = grupoRepository.findByCategoriaEixo(categoria)
                .orElseGet(() -> new GrupoRegulamento(categoria, request.limiteMaximoHoras()));
        grupo.atualizarLimite(request.limiteMaximoHoras());
        try {
            return GrupoRegulamentoResponse.from(grupoRepository.saveAndFlush(grupo));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O eixo foi configurado por outra solicitação; atualize e tente novamente");
        }
    }
}
