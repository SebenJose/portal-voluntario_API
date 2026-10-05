package portal_voluntario.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import portal_voluntario.domain.StatusParticipacao;

public record PresencaRequest(@NotEmpty List<@NotNull @Valid PresencaItem> participantes) {
    public record PresencaItem(
            @NotNull @Positive Long inscricaoId,
            @NotNull StatusParticipacao status) {
    }
}
