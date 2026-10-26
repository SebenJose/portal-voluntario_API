package portal_voluntario.service;

import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import portal_voluntario.domain.StatusParticipacao;

@Service
public class RelatorioService {
    private final InscricaoService inscricoes;
    private final DashboardService dashboard;

    public RelatorioService(InscricaoService inscricoes, DashboardService dashboard) {
        this.inscricoes = inscricoes;
        this.dashboard = dashboard;
    }

    public byte[] participantes(Long oportunidadeId, StatusParticipacao status, String busca, Authentication auth) {
        var participantes = inscricoes.participantes(oportunidadeId, status, auth);
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        RelatorioCsv csv = new RelatorioCsv();
        csv.linha("Oportunidade ID", "Oportunidade", "Inscrição ID", "Participante ID", "Nome", "E-mail", "Presença", "Inscrito em");
        participantes.stream().filter(item -> (item.usuarioNome() + " " + item.usuarioEmail())
                .toLowerCase(Locale.ROOT).contains(termo)).forEach(item -> csv.linha(
                        item.oportunidadeId(), item.oportunidadeTitulo(), item.id(), item.usuarioId(),
                        item.usuarioNome(), item.usuarioEmail(), item.statusParticipacao(), item.dataInscricao()));
        return csv.bytes();
    }

    public byte[] bancoHoras() {
        var estudantes = dashboard.consultarEstudantes();
        RelatorioCsv csv = new RelatorioCsv();
        csv.linha("Estudante ID", "Nome", "E-mail", "Matrícula RA", "Conta ativa", "Eixo",
                "Horas acumuladas", "Limite de horas", "Horas computadas", "Horas excedentes", "Percentual atingido", "Limite atingido");
        estudantes.forEach(item -> item.bancoHoras().eixos().forEach(eixo -> csv.linha(
                item.usuarioId(), item.nome(), item.email(), item.matriculaRa(), item.ativo(), eixo.categoriaEixo(),
                eixo.horasAcumuladas(), eixo.limiteMaximoHoras(), eixo.horasComputadas(), eixo.horasExcedentes(),
                eixo.percentualAtingido(), eixo.limiteAtingido())));
        return csv.bytes();
    }
}
