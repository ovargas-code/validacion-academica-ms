package co.edu.udemedellin.validacionacademica.application.usecase

import co.edu.udemedellin.validacionacademica.domain.model.*
import co.edu.udemedellin.validacionacademica.domain.ports.DocumentGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.domain.ports.StudentRepositoryPort
import co.edu.udemedellin.validacionacademica.domain.ports.ValidationRepositoryPort
import org.springframework.stereotype.Service
import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CreateValidationUseCase(
    private val validationRepositoryPort: ValidationRepositoryPort,
    private val studentRepositoryPort: StudentRepositoryPort,
    private val documentGeneratorPort: DocumentGeneratorPort,
    private val pdfGeneratorPort: PdfGeneratorPort,
    private val mailPort: MailPort
) {
    fun execute(request: ValidationRequest): ValidationExecutionResponse {
        // 1. Generamos el código único de verificación
        val generatedCode = "UDEM-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

        // 2. Guardamos la solicitud con el código generado
        val requestWithCode = request.copy(verificationCode = generatedCode)
        val savedRequest = validationRepositoryPort.save(requestWithCode)

        // 3. Buscamos al estudiante por documento
        val student = studentRepositoryPort.findByDocument(savedRequest.studentDocument)

        // 4. Generamos el resultado de la validación
        val result = buildResult(savedRequest.id!!, savedRequest.validationType, student)

        // 5. Generamos la carta en texto plano
        val letter = documentGeneratorPort.generateLetter(savedRequest, student, result)

        // 6. Si el estudiante es válido, generamos PDF y enviamos correo
        if (student != null && result.status == ValidationStatus.VALID) {
            try {
                // Usamos el programa REAL del estudiante (no hardcodeado)
                val pdfBytes = pdfGeneratorPort.generateCertificate(
                    studentName = student.fullName,
                    studentDocument = student.document,
                    program = student.program,
                    verificationCode = savedRequest.verificationCode
                )

                mailPort.enviarCertificado(
                    emailDestino = savedRequest.requesterEmail,
                    nombreEstudiante = student.fullName,
                    pdfBytes = pdfBytes
                )

                println(">>> ÉXITO: Certificado enviado a ${savedRequest.requesterEmail}")

            } catch (e: Exception) {
                // Logueamos el error pero no interrumpimos el flujo
                println(">>> ERROR en notificación por correo: ${e.message}")
            }
        }

        return ValidationExecutionResponse(
            request = savedRequest,
            result = result,
            letter = letter,
            student = student   // <-- Ahora incluimos el estudiante en la respuesta
        )
    }

    private fun buildResult(
        requestId: Long,
        validationType: ValidationType,
        student: Student?
    ): ValidationResult {
        val controlCode = "VAL-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"

        if (student == null) {
            return ValidationResult(
                requestId = requestId,
                status = ValidationStatus.NOT_FOUND,
                controlCode = controlCode,
                message = "No se encontró información académica asociada al documento consultado."
            )
        }

        return when (validationType) {
            ValidationType.DEGREE -> {
                if (student.status == StudentStatus.GRADUATED) {
                    ValidationResult(requestId, ValidationStatus.VALID, controlCode,
                        "Se valida que la persona sí obtuvo el título registrado.")
                } else {
                    ValidationResult(requestId, ValidationStatus.REQUIRES_REVIEW, controlCode,
                        "La persona existe, pero no figura como graduada.")
                }
            }
            ValidationType.ENROLLMENT -> {
                if (student.status == StudentStatus.ACTIVE) {
                    ValidationResult(requestId, ValidationStatus.VALID, controlCode,
                        "Se valida que la persona se encuentra con matrícula activa.")
                } else {
                    ValidationResult(requestId, ValidationStatus.REQUIRES_REVIEW, controlCode,
                        "La persona existe, pero no registra matrícula activa.")
                }
            }
        }
    }
}

// Agregamos 'student' al response para que el controller pueda usar el programa real
data class ValidationExecutionResponse(
    val request: ValidationRequest,
    val result: ValidationResult,
    val letter: String,
    val student: Student?   // <-- Nuevo campo
)
