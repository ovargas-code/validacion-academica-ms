# Validacion Academica - Microservicio

Microservicio REST con **Spring Boot 3 + Kotlin** para validar titulos y matriculas de estudiantes. Genera certificados PDF con QR y los envia por correo.

---

## Tecnologias

- **Kotlin 1.9** + **Java 21**
- **Spring Boot 3.3** (Web, JPA, Security, Mail, Thymeleaf)
- **PostgreSQL 16** (produccion) / **H2** (desarrollo local)
- **OpenPDF + ZXing** (PDF y QR)
- **Swagger / SpringDoc OpenAPI 2.6**
- **Docker + Docker Compose**

---

## Modos de ejecucion

### MODO 1 - Solo H2 en memoria (mas facil, no necesita nada instalado)

```powershell
./gradlew bootRun
```

- La app arranca en http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **H2 Console:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:validaciondb`
  - Usuario: `sa` / Sin contrasena

Los datos se pierden al reiniciar (es solo para pruebas rapidas).

---

### MODO 2 - App local + PostgreSQL en Docker (recomendado para desarrollo)

Este modo usa la BD real de PostgreSQL pero ejecuta la app directamente en tu maquina (mas facil de depurar).

**Paso 1** - Levantar SOLO el contenedor de PostgreSQL:
```powershell
docker-compose up postgres -d
```

**Paso 2** - Correr la app apuntando a ese PostgreSQL:
```powershell
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

- La app arranca en http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html

Para detener PostgreSQL cuando termines:
```powershell
docker-compose down
```

---

### MODO 3 - Todo en Docker (produccion / demo completa)

Este modo levanta tanto PostgreSQL como la app dentro de Docker.

```powershell
docker-compose up --build
```

- La app arranca en http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html

Para detener todo:
```powershell
docker-compose down
```

> **Nota:** Si ya tienes la app corriendo en MODO 2 ocupando el puerto 8080, detente con Ctrl+C antes de usar MODO 3.

---

## Configuracion de correo (opcional)

Para que el envio de correos funcione, copia el archivo de ejemplo y llena tus credenciales de Gmail:

```powershell
cp .env.example .env
```

Edita `.env` con tu correo y App Password de Gmail. Si no configuras esto, la app funciona igual pero no envia correos.

---

## Endpoints principales

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| POST | `/api/v1/students` | Registrar estudiante |
| GET | `/api/v1/students` | Listar todos los estudiantes |
| GET | `/api/v1/students/{document}` | Buscar por documento |
| POST | `/api/validations/verify` | Solicitar validacion (genera PDF) |
| GET | `/api/v1/verificaciones/{code}` | Verificar certificado (JSON) |
| GET | `/api/v1/verificaciones/{code}/pdf` | Descargar certificado (PDF) |

Documentacion completa en **Swagger UI**: http://localhost:8080/swagger-ui.html

---

## Arquitectura

El proyecto sigue **Arquitectura Hexagonal** (Ports & Adapters):

```
domain/          - Modelos y puertos (interfaces). Sin dependencias externas.
application/     - Casos de uso. Orquestan la logica de negocio.
infrastructure/  - Adaptadores: REST controllers, JPA, Email, PDF.
bootstrap/       - Punto de entrada de Spring Boot.
```

---

## Tests

```powershell
./gradlew test
```

---

## Seguridad

- Las credenciales (BD, correo, admin) se configuran con **variables de entorno**.
- El archivo `.env` esta en `.gitignore` y nunca debe subirse a Git.
- Spring Security protege el portal administrativo.
- La API REST y Swagger son publicos para pruebas.
