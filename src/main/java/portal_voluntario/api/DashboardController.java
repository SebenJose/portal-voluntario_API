package portal_voluntario.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.BancoHorasResponse;
import portal_voluntario.service.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/horas")
    public BancoHorasResponse horas(Authentication authentication) {
        return dashboardService.consultar(authentication.getName());
    }
}
