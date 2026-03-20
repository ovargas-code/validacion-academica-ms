package co.edu.udemedellin.validacionacademica.infrastructure.email

import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.core.io.ByteArrayResource
import jakarta.mail.internet.MimeMessage

@Component
class MailServiceAdapter(private val mailSender: JavaMailSender) : MailPort {

    override fun enviarCertificado(emailDestino: String, nombreEstudiante: String, pdfBytes: ByteArray) {
        val mensaje: MimeMessage = mailSender.createMimeMessage()

        // 'true' permite adjuntar archivos
        val helper = MimeMessageHelper(mensaje, true, "UTF-8")

        helper.setTo(emailDestino)
        helper.setSubject("Certificado de Graduado - Universidad de Medellín")

        val cuerpoHtml = """
            <div style="font-family: Arial, sans-serif; border-top: 5px solid #C8102E; padding: 20px; color: #333;">
                <h2 style="color: #C8102E;">¡Cordial saludo, $nombreEstudiante!</h2>
                <p>Adjunto encontrará su <b>Certificado de Graduado</b> oficial, emitido por la Universidad de Medellín.</p>
                <p>Este documento cuenta con firma digital y validación institucional mediante código QR.</p>
                <br>
                <p>Atentamente,</p>
                <p><b>SANDRA PATRICIA GIRALDO MONTOYA</b><br>
                Coordinadora de Admisiones y Registro<br>
                Universidad de Medellín</p>
            </div>
        """.trimIndent()

        helper.setText(cuerpoHtml, true)

        // Usamos el nombre del estudiante para el nombre del archivo PDF
        val nombreArchivo = "Certificado_${nombreEstudiante.replace(" ", "_")}.pdf"
        helper.addAttachment(nombreArchivo, ByteArrayResource(pdfBytes))

        mailSender.send(mensaje)
    }
}