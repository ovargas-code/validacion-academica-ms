package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.CreateValidationUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.GetStudentByDocumentUseCase
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class WebController(
    private val createValidationUseCase: CreateValidationUseCase,
    private val getStudentByDocumentUseCase: GetStudentByDocumentUseCase
) {

    @GetMapping("/")
    fun index(): String {
        return "index"
    }

    @PostMapping("/verificar")
    fun verify(
        @RequestParam document: String,
        @RequestParam email: String,
        model: Model
    ): String {

        // 1. Buscamos al estudiante en PostgreSQL antes de crear la validación
        val studentFound = getStudentByDocumentUseCase.execute(document)

        // 2. Si existe, tomamos su nombre completo; si no, usamos un respaldo
        val nameToRegister = studentFound?.fullName ?: "Portal Web"

        // 3. Creamos la solicitud con el nombre real para que aparezca en el PDF
        val request = ValidationRequest(
            requesterName = nameToRegister,
            requesterEmail = email,
            studentDocument = document,
            validationType = ValidationType.DEGREE,
            verificationCode = ""
        )

        // 4. Ejecutamos la validación (esto genera el registro y envía el correo)
        val response = createValidationUseCase.execute(request)

        // 5. Enviamos los datos a la vista result.html
        model.addAttribute("status", response.result.status)
        model.addAttribute("message", response.result.message)
        model.addAttribute("code", response.request.verificationCode)
        model.addAttribute("letter", response.letter)

        return "result"
    }
}