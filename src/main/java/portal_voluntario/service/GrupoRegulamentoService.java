package portal_voluntario.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.api.dto.GrupoRegulamentoResponse;
import portal_voluntario.repository.GrupoRegulamentoRepository;

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
}
