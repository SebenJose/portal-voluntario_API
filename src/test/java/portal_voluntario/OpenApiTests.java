package portal_voluntario;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    JsonNode documento() throws Exception {
        var response = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        return mapper.readTree(response.getContentAsByteArray());
    }

    JsonNode resolver(JsonNode documento, JsonNode schema) {
        return schema.has("$ref") ? documento.at(schema.path("$ref").asString().substring(1)) : schema;
    }

    @Test
    void documentacaoPublicaConfiguraJwtSemPersistirCredenciais() throws Exception {
        JsonNode doc = documento();
        assertThat(doc.path("info").path("title").asString()).isEqualTo("API Portal Voluntário");
        assertThat(doc.at("/components/securitySchemes/bearerAuth/scheme").asString()).isEqualTo("bearer");
        assertThat(doc.at("/components/securitySchemes/bearerAuth/bearerFormat").asString()).isEqualTo("JWT");
        assertThat(doc.path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(doc.path("servers").get(0).path("url").asString()).isEqualTo("/");
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(get("/swagger-ui/swagger-ui.css")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.persistAuthorization").value(false))
                .andExpect(jsonPath("$.validatorUrl").value(""));
        byte[] yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(yaml, StandardCharsets.UTF_8)).contains("API Portal Voluntário");
    }

    @Test
    void operacoesPublicasDispensamJwtEPrivadasContinuamProtegidas() throws Exception {
        JsonNode doc = documento();
        for (String[] operacao : new String[][] {
                {"/api/v1/auth/registrar", "post"}, {"/api/v1/auth/login", "post"},
                {"/api/v1/auth/recuperar-senha", "post"}, {"/api/v1/auth/redefinir-senha", "post"},
                {"/api/v1/oportunidades", "get"}, {"/api/v1/oportunidades/{id}", "get"},
                {"/api/v1/grupos-regulamento", "get"}, {"/api/v1/certificados/verificar/{codigo}", "get"} }) {
            JsonNode seguranca = doc.path("paths").path(operacao[0]).path(operacao[1]).path("security");
            assertThat(seguranca.isArray()).as(operacao[0]).isTrue();
            assertThat(seguranca.size()).as(operacao[0]).isZero();
        }
        for (String path : List.of("/api/v1/conta", "/api/v1/admin/usuarios", "/api/v1/dashboard/horas",
                "/api/v1/oportunidades/{oportunidadeId}/inscricoes", "/api/v1/admin/relatorios/banco-horas")) {
            JsonNode security = doc.path("paths").path(path).path("get").path("security");
            if (security.isMissingNode()) security = doc.path("security");
            assertThat(security.get(0).has("bearerAuth")).as(path).isTrue();
        }
        mvc.perform(get("/api/v1/conta")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void contratoIncluiTodasAsRotasReaisSemParametrosInternosDeAutenticacao() throws Exception {
        JsonNode doc = documento();
        Set<String> ids = new HashSet<>();
        int quantidade = 0;
        for (var item : mappings.getHandlerMethods().entrySet()) {
            var mapping = item.getKey();
            for (String path : mapping.getPatternValues()) {
                if (!path.startsWith("/api/v1/")) continue;
                for (var method : mapping.getMethodsCondition().getMethods()) {
                    JsonNode operacao = doc.path("paths").path(path).path(method.name().toLowerCase(Locale.ROOT));
                    assertThat(operacao.isMissingNode()).as(method + " " + path).isFalse();
                    assertThat(operacao.path("summary").asString()).as(path).isNotBlank();
                    ResponseStatus resposta = item.getValue().getMethodAnnotation(ResponseStatus.class);
                    String sucesso = resposta == null ? "200" : Integer.toString(resposta.code().value());
                    assertThat(operacao.path("responses").has(sucesso)).as("Resposta " + sucesso + " em " + method + " " + path).isTrue();
                    if (item.getValue().getMethod().getReturnType() != void.class) {
                        assertThat(operacao.path("responses").path(sucesso).path("content").size())
                                .as("Corpo da resposta de " + method + " " + path).isPositive();
                    }
                    assertThat(ids.add(operacao.path("operationId").asString())).as("operationId único em " + path).isTrue();
                    for (JsonNode parametro : operacao.path("parameters")) {
                        assertThat(parametro.path("name").asString()).isNotIn("auth", "authentication", "principal");
                    }
                    quantidade++;
                }
            }
        }
        assertThat(quantidade).isGreaterThan(40);
        assertThat(doc.path("paths").has("/error")).isFalse();
        assertThat(doc.path("components").path("schemas").has("Usuario")).isFalse();
    }

    @Test
    void contratoDistingueMultipartPdfImagensECsv() throws Exception {
        JsonNode doc = documento();
        JsonNode upload = doc.path("paths").path("/api/v1/documentos").path("post");
        JsonNode arquivo = resolver(doc, upload.path("requestBody").path("content").path("multipart/form-data").path("schema"))
                .path("properties").path("arquivo");
        assertThat(arquivo.path("type").asString()).isEqualTo("string");
        assertThat(arquivo.path("format").asString()).isEqualTo("binary");
        for (String[] download : new String[][] {
                {"/api/v1/certificados/{codigo}/pdf", "application/pdf"},
                {"/api/v1/documentos/{id}", "image/png"}, {"/api/v1/documentos/{id}", "image/jpeg"},
                {"/api/v1/oportunidades/{id}/inscricoes/exportacao", "text/csv"},
                {"/api/v1/admin/relatorios/banco-horas/exportacao", "text/csv"} }) {
            JsonNode content = doc.path("paths").path(download[0]).path("get").path("responses").path("200").path("content");
            assertThat(content.path(download[1]).path("schema").path("format").asString()).as(download[0]).isEqualTo("binary");
            assertThat(content.has("application/json")).as(download[0]).isFalse();
        }
    }

    @Test
    void contratoDocumentaSenhasObrigatoriasVersaoDaRevisaoEStatusSemCorpo() throws Exception {
        JsonNode doc = documento();
        for (String[] campo : new String[][] { {"RegisterRequest", "senha"}, {"AdminUserRequest", "senha"},
                {"AlterarSenhaRequest", "novaSenha"}, {"RedefinirSenhaRequest", "novaSenha"} }) {
            JsonNode schema = doc.path("components").path("schemas").path(campo[0]);
            JsonNode senha = schema.path("properties").path(campo[1]);
            assertThat(senha.path("format").asString()).isEqualTo("password");
            assertThat(senha.path("minLength").asInt()).isEqualTo(8);
            assertThat(senha.path("maxLength").asInt()).isEqualTo(72);
            assertThat(senha.path("writeOnly").asBoolean()).isTrue();
            assertThat(schema.path("required").toString()).contains('"' + campo[1] + '"');
        }
        JsonNode revisao = doc.path("components").path("schemas").path("RevisaoSubmissaoRequest");
        assertThat(revisao.path("required").toString()).contains("justificativa", "versaoEsperada", "statusValidacao");
        assertThat(revisao.path("properties").path("statusValidacao").path("enum").toString())
                .contains("APROVADA", "REJEITADA").doesNotContain("PENDENTE");
        assertThat(doc.path("components").path("schemas").path("PresencaItem").path("properties").path("status").path("enum").toString())
                .contains("PRESENTE", "AUSENTE").doesNotContain("INSCRITO");
        JsonNode respostasRevisao = doc.path("paths").path("/api/v1/admin/submissoes-externas/{id}").path("patch").path("responses");
        assertThat(respostasRevisao.path("409").path("$ref").asString()).isEqualTo("#/components/responses/Conflito");
        assertThat(respostasRevisao.path("401").path("$ref").asString()).isEqualTo("#/components/responses/NaoAutenticado");
        assertThat(respostasRevisao.path("400").path("$ref").asString()).isEqualTo("#/components/responses/ErroValidacao");
        for (String path : List.of("/api/v1/auth/redefinir-senha", "/api/v1/conta/senha")) {
            String method = path.contains("redefinir") ? "post" : "patch";
            JsonNode response = doc.path("paths").path(path).path(method).path("responses").path("204");
            assertThat(response.isMissingNode()).isFalse();
            assertThat(response.has("content")).isFalse();
        }
    }
}
