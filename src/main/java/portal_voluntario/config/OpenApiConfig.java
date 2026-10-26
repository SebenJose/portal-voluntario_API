package portal_voluntario.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Arrays;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI portalOpenApi() {
        return new OpenAPI()
                .info(new Info().title("API Portal Voluntário").version("v1")
                        .description("""
                                Oportunidades, inscrições, horas e certificados de atividades voluntárias.

                                Faça login em POST /api/v1/auth/login e informe o accessToken em Authorize.
                                As operações indicam os perfis permitidos: ESTUDANTE, ORGANIZADOR e ADMINISTRADOR.
                                A gestão exige o organizador responsável ou um administrador.
                                Alterar a senha ou desativar a conta invalida os JWTs anteriores.

                                Datas usam ISO 8601 com fuso horário. Horas aceitam até duas casas decimais.
                                Erros de negócio retornam ProblemDetail com a explicação em detail;
                                rejeições pelo filtro de segurança podem retornar corpo vazio.
                                """))
                .addServersItem(new Server().url("/").description("Servidor atual"))
                .components(componentes().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Cole somente o accessToken retornado pelo login, sem o prefixo Bearer.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OperationCustomizer respostasComuns() {
        return (operacao, metodo) -> {
            if (!metodo.hasMethodAnnotation(SecurityRequirements.class)) {
                operacao.getResponses().putIfAbsent("401", referencia("NaoAutenticado"));
                operacao.getResponses().putIfAbsent("403", referencia("SemPermissao"));
            }
            boolean recebeDados = Arrays.stream(metodo.getMethodParameters()).anyMatch(parametro ->
                    parametro.hasParameterAnnotation(RequestBody.class) || parametro.hasParameterAnnotation(RequestParam.class)
                            || parametro.hasParameterAnnotation(PathVariable.class));
            if (recebeDados) operacao.getResponses().putIfAbsent("400", referencia("ErroValidacao"));
            return operacao;
        };
    }

    private Components componentes() {
        return new Components().addSchemas("Problema", new ObjectSchema()
                .addProperty("type", new StringSchema().format("uri")).addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema()).addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema().format("uri-reference")))
                .addResponses("ErroValidacao", problema("Dados inválidos, campos ausentes ou parâmetros incompatíveis. O corpo pode estar vazio para erros de conversão."))
                .addResponses("NaoAutenticado", problema("Credenciais inválidas ou JWT ausente, expirado ou revogado. O filtro de segurança pode retornar corpo vazio."))
                .addResponses("SemPermissao", problema("O perfil ou a conta não tem permissão para esta operação. O filtro de segurança pode retornar corpo vazio."))
                .addResponses("NaoEncontrado", problema("Registro inexistente ou indisponível para a conta autenticada."))
                .addResponses("Conflito", problema("A operação conflita com o estado atual. Consulte detail e atualize os dados antes de tentar novamente."))
                .addResponses("LimiteUpload", problema("A requisição multipart excedeu o limite permitido. Envie um arquivo de até 5 MB."));
    }

    private ApiResponse problema(String descricao) {
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Problema"))));
    }

    private ApiResponse referencia(String nome) {
        return new ApiResponse().$ref("#/components/responses/" + nome);
    }
}
