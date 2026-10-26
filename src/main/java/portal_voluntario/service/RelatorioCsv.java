package portal_voluntario.service;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

/** CSV com BOM UTF-8, separador de planilhas pt-BR e células textuais protegidas. */
final class RelatorioCsv {
    private final StringBuilder conteudo = new StringBuilder("\uFEFF");

    void linha(Object... valores) {
        conteudo.append(Arrays.stream(valores).map(RelatorioCsv::celula)
                .collect(Collectors.joining(";"))).append("\r\n");
    }

    byte[] bytes() {
        return conteudo.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String celula(Object valor) {
        String texto = valor instanceof BigDecimal decimal ? decimal.setScale(2).toPlainString().replace('.', ',')
                : valor == null ? "" : valor.toString();
        if (valor instanceof CharSequence) {
            String inicio = texto.stripLeading();
            if ((!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0)
                    || texto.indexOf('\t') >= 0 || texto.indexOf('\r') >= 0 || texto.indexOf('\n') >= 0) {
                texto = "'" + texto;
            }
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
