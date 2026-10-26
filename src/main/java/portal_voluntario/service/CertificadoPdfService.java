package portal_voluntario.service;

import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import portal_voluntario.domain.CertificadoEmitido;

@Service
public class CertificadoPdfService {
    public byte[] gerar(CertificadoEmitido certificado, String verificacaoUrl) throws IOException {
        try (PDDocument document = new PDDocument();
                InputStream fonte = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {
            PDType0Font font = PDType0Font.load(document, fonte);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            document.getDocumentInformation().setTitle("Certificado de participação — Portal Voluntário");
            document.getDocumentInformation().setAuthor("Portal Voluntário");
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setNonStrokingColor(1f, .82f, .15f);
                content.addRect(45, 745, 505, 7); content.fill();
                content.setNonStrokingColor(.08f, .08f, .08f);
                float y = linha(content, font, "PORTAL VOLUNTÁRIO", 14, 710);
                y = linha(content, font, "Certificado de participação", 24, y - 25);
                y = linha(content, font, "Certificamos a participação de", 12, y - 30);
                y = linha(content, font, certificado.getBeneficiario(), 20, y - 8);
                y = linha(content, font, "na atividade “" + certificado.getAtividade() + "”.", 14, y - 12);
                y = linha(content, font, "Organização: " + certificado.getOrganizador(), 12, y - 20);
                y = linha(content, font, "Carga horária: " + certificado.getHoras().stripTrailingZeros().toPlainString() + " horas", 12, y - 8);
                y = linha(content, font, "Eixo: " + certificado.getCategoriaEixo().name(), 12, y - 8);
                DateTimeFormatter data = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));
                y = linha(content, font, "Período: " + data.format(certificado.getDataInicio()) + " a " + data.format(certificado.getDataFim()) + " (UTC−3)", 11, y - 8);
                y = linha(content, font, "Emitido em: " + data.format(certificado.getEmitidoEm()), 11, y - 8);
                if (certificado.getCodigoAnterior() != null) {
                    y = linha(content, font, "Retificação nº " + certificado.getVersao() + " — substitui o certificado:", 10, y - 8);
                    y = linha(content, font, certificado.getCodigoAnterior().toString(), 10, y - 8);
                }
                y = linha(content, font, "Código de verificação: " + certificado.getCodigo(), 10, y - 35);
                linha(content, font, "Confira os dados e a validade no endereço: " + verificacaoUrl, 10, y - 8);
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    private float linha(PDPageContentStream content, PDType0Font font, String value, int size, float y) throws IOException {
        String text = value.replaceAll("\\p{Cntrl}", " ");
        StringBuilder line = new StringBuilder();
        for (int cp : text.codePoints().toArray()) {
            String character = new String(Character.toChars(cp));
            if (font.getStringWidth(line + character) * size / 1000 > 495 && !line.isEmpty()) {
                escrever(content, font, line.toString(), size, y); y -= size + 7; line.setLength(0);
            }
            line.append(character);
        }
        if (!line.isEmpty()) { escrever(content, font, line.toString(), size, y); y -= size + 7; }
        return y;
    }
    private void escrever(PDPageContentStream content, PDType0Font font, String text, int size, float y) throws IOException {
        content.beginText(); content.setFont(font, size); content.newLineAtOffset(50, y); content.showText(text); content.endText();
    }
}
