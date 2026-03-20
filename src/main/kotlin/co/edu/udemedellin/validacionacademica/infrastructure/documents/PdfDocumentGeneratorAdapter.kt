package co.edu.udemedellin.validacionacademica.infrastructure.documents

import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import com.lowagie.text.*
import com.lowagie.text.pdf.*
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.awt.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter

@Component
class PdfDocumentGeneratorAdapter : PdfGeneratorPort {

    override fun generateCertificate(
        studentName: String,
        studentDocument: String,
        program: String,
        verificationCode: String
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 72f, 72f, 72f, 72f)
        PdfWriter.getInstance(document, out)
        document.open()

        val fontTitle    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, Color(200, 16, 46))
        val fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
        val fontBody     = FontFactory.getFont(FontFactory.HELVETICA, 12f)
        val fontBold     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)
        val fontFooter   = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9f)

        // --- 1. ENCABEZADO - cargado desde classpath (funciona en Docker y local) ---
        try {
            val cabezoteStream = javaClass.getResourceAsStream("/static/images/cabezote_rojo.png")
            if (cabezoteStream != null) {
                val cabezote = Image.getInstance(cabezoteStream.readBytes())
                cabezote.scaleToFit(PageSize.A4.width, 100f)
                cabezote.setAbsolutePosition(0f, PageSize.A4.height - 100f)
                document.add(cabezote)
            }
            document.add(Paragraph("\n\n\n"))

            val logoStream = javaClass.getResourceAsStream("/static/images/logo_udem.png")
            if (logoStream != null) {
                val logo = Image.getInstance(logoStream.readBytes())
                logo.scaleToFit(175f, 175f)
                logo.alignment = Element.ALIGN_CENTER
                document.add(logo)
            }
        } catch (e: Exception) {
            document.add(Paragraph("\n\n\n\n\n"))
        }

        document.add(Paragraph("\n\n"))

        // --- 2. CUERPO ---
        val title = Paragraph("CERTIFICADO DE GRADUADO", fontTitle)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        val header = Paragraph("LA COORDINACIÓN DE ADMISIONES Y REGISTRO DE LA UNIVERSIDAD DE MEDELLÍN,", fontSubtitle)
        header.alignment = Element.ALIGN_CENTER
        document.add(header)

        document.add(Paragraph("\n\nHACE CONSTAR QUE:", fontBody))
        document.add(Paragraph("\n"))

        val namePara = Paragraph(studentName.uppercase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f))
        namePara.alignment = Element.ALIGN_CENTER
        document.add(namePara)

        val bodyText = Paragraph()
        bodyText.alignment = Element.ALIGN_JUSTIFIED
        bodyText.spacingBefore = 15f
        bodyText.add(Chunk("Identificado(a) con documento de identidad No. ", fontBody))
        bodyText.add(Chunk(studentDocument, fontBold))
        bodyText.add(Chunk(", cumplió satisfactoriamente los requisitos exigidos para optar al título de:\n\n", fontBody))
        document.add(bodyText)

        val progPara = Paragraph(program.uppercase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f))
        progPara.alignment = Element.ALIGN_CENTER
        document.add(progPara)

        val dateStr = SimpleDateFormat("dd 'días del mes de' MMMM 'de' yyyy", Locale("es", "CO")).format(Date())
        document.add(Paragraph("\n\nSe expide en Medellín, a los $dateStr.\n\n", fontBody))

        // --- 3. FIRMA Y QR ---
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(65f, 35f))

        val cellFirma = PdfPCell()
        cellFirma.border = Rectangle.NO_BORDER
        cellFirma.verticalAlignment = Element.ALIGN_BOTTOM

        try {
            val firmaStream = javaClass.getResourceAsStream("/static/images/firma_sandra.png")
            if (firmaStream != null) {
                val imgFirma = Image.getInstance(firmaStream.readBytes())
                imgFirma.scaleToFit(260f, 130f)
                imgFirma.alignment = Element.ALIGN_CENTER
                imgFirma.spacingBefore = 10f
                imgFirma.spacingAfter = -8f
                cellFirma.addElement(imgFirma)
            }
        } catch (e: Exception) {}

        // Linea subrayada calibrada al ancho del nombre
        val lineChunk = Chunk("                                        ", fontBold)
        lineChunk.setUnderline(0.8f, 2f)
        val linePara = Paragraph()
        linePara.alignment = Element.ALIGN_CENTER
        linePara.add(lineChunk)
        linePara.spacingAfter = 4f
        cellFirma.addElement(linePara)

        val signInfo = Paragraph("SANDRA PATRICIA GIRALDO MONTOYA\nCoordinadora\nCoordinación De Admisiones Y Registro", fontBold)
        signInfo.alignment = Element.ALIGN_CENTER
        signInfo.spacingBefore = 2f
        cellFirma.addElement(signInfo)
        table.addCell(cellFirma)

        val cellQr = PdfPCell()
        cellQr.border = Rectangle.NO_BORDER
        cellQr.verticalAlignment = Element.ALIGN_BOTTOM
        try {
            val qrWriter = QRCodeWriter()
            val bitMatrix = qrWriter.encode(
                "https://validador.udem.edu.co/verify/$verificationCode",
                BarcodeFormat.QR_CODE, 200, 200
            )
            val qrStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream)
            val qrImg = Image.getInstance(qrStream.toByteArray())
            qrImg.scaleToFit(90f, 90f)
            qrImg.alignment = Element.ALIGN_RIGHT
            cellQr.addElement(qrImg)
            val qrTxt = Paragraph("Verificación: $verificationCode", FontFactory.getFont(FontFactory.HELVETICA, 7f))
            qrTxt.alignment = Element.ALIGN_RIGHT
            cellQr.addElement(qrTxt)
        } catch (e: Exception) {}
        table.addCell(cellQr)

        document.add(table)

        // --- 4. PIE DE PÁGINA ---
        val footer = Paragraph("\n\nEste documento es una copia auténtica. Verifique en validador.udem.edu.co\nUniversidad de Medellín - Vigilada Mineducación.", fontFooter)
        footer.alignment = Element.ALIGN_CENTER
        document.add(footer)

        document.close()
        return out.toByteArray()
    }
}
