package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.BancoHorasResponse;
import portal_voluntario.service.DashboardService;

@Tag(name = "Banco de horas", description = "Saldo aproveitado e excedente por eixo.")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/horas")
    @Operation(summary = "Consultar meu banco de horas",
            description = "ESTUDANTE. Soma registros internos não revogados e comprovantes externos aprovados com eixo. Aplica o teto atual por eixo e preserva o acumulado e o excedente.")
    public BancoHorasResponse horas(Authentication authentication) {
        return dashboardService.consultar(authentication.getName());
    }
}
