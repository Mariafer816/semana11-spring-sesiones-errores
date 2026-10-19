# Sistema de Reservas de Aulas

Aplicación web para gestionar aulas, cursos, usuarios y reservas, con inicio de sesión y registro de cuentas.

**Stack:** Java 17 · Spring Boot 3.5 · Spring MVC + Thymeleaf · Spring Data JPA · Spring Security · H2 / MySQL 8

## Ejecución

Requisito: Java 17. **No hace falta instalar ninguna base de datos.**

1. Ejecute `./mvnw spring-boot:run` (o ejecute `ReservasAulasApplication` desde IntelliJ).
2. Espere el mensaje `Started ReservasAulasApplication in ... seconds`.
3. Abra http://localhost:8080 — le llevará al inicio de sesión.

Flujo: **Login → Menú principal**. Si no tiene cuenta, el botón **Crear cuenta nueva** del login lleva al registro,
que crea la cuenta, inicia la sesión y entra directamente al menú.

### Base de datos SQL

| Modo | Cómo activarlo | Datos |
|---|---|---|
| **H2 en archivo** (por defecto) | No hace nada: ya viene configurado | Se **conservan** entre ejecuciones (carpeta `./data`) |
| **MySQL** | Perfil `mysql`: en IntelliJ, *Run > Edit Configurations > Active profiles = mysql*; por consola `-Dspring-boot.run.profiles=mysql` | Se conservan en MySQL |

Para MySQL: enciéndalo antes (por ejemplo desde XAMPP). La base `reservas_aulas` y las tablas se crean solas.
Si `root` tiene contraseña, defina la variable de entorno `DB_PASSWORD`.
El script `database/schema.sql` contiene el esquema MySQL equivalente por si se prefiere crearlo manualmente.

Para empezar con la base de datos vacía en modo H2, detenga la aplicación y borre la carpeta `data`.

## Si la aplicación no arranca

| Síntoma en la consola | Causa | Solución |
|---|---|---|
| `Communications link failure` / `Connection refused` | Está usando el perfil `mysql` y MySQL no está encendido | Encienda MySQL, o quite el perfil `mysql` para usar H2 |
| `Access denied for user 'root'` | MySQL tiene contraseña | Defina la variable `DB_PASSWORD` |
| `Port 8080 was already in use` | Otro programa usa el puerto | Cierre ese programa o cambie `server.port` en `application.properties` |
| `Database may be already in use` | Hay otra copia de la aplicación abierta | Detenga la otra ejecución (botón rojo de IntelliJ) |

## Inicio de sesión y registro

| Ruta         | Descripción                                                                    |
|--------------|--------------------------------------------------------------------------------|
| `/login`     | Ingreso con correo y contraseña; botón **Crear cuenta nueva**; al entrar lleva al menú |
| `/registro`  | Crea una cuenta nueva, **o** asigna contraseña a un usuario existente sin ella, e **inicia la sesión automáticamente** |
| `/logout`    | Cierra la sesión (botón en el menú)                                            |

- Las contraseñas se guardan con **BCrypt** en la columna `usuarios.password`; nunca en texto plano.
- Política: mínimo 8 caracteres, al menos una letra y un número.
- Los usuarios de ejemplo (sembrados al primer arranque) no tienen contraseña: la crean desde `/registro`.
- La API REST (`/api/**`) usa HTTP Basic con el mismo correo y contraseña.

## Estructura del proyecto

```
src/main/java/Sistema/Reservas/Aulas
├── Aspect/        Registro (log) de la ejecución de los servicios
├── Controller/    Controladores web (Thymeleaf) y REST (/api); ModeloGlobalAdvice da tema y usuario activo a todas las vistas
├── Model/         Entidades JPA y formularios (CursoForm, ReservaForm, RegistroForm)
├── Repository/    Acceso a datos (Spring Data JPA)
├── Security/      Configuración de seguridad, login, registro y manejo de la HttpSession (entrada y salida)
├── Service/       Lógica de negocio
├── Util/          Utilidades (paginación, sesión)
├── Validation/    Validaciones propias reutilizables (@ContrasenaSegura, @PasswordsCoinciden)
└── ReservasAulasApplication.java

src/main/resources
├── templates/     Vistas Thymeleaf (login, registro, módulos) y fragments/componentes.html
├── static/css/    Hojas de estilo
└── application*.properties   Configuración (H2 en archivo por defecto; perfil mysql opcional)

database/schema.sql          Esquema SQL de MySQL
```

Flujo de capas: `Controller → Service → Repository → Base de datos SQL`.
`Security` agrupa lo relacionado con acceso; queda fuera de `Service` a propósito, porque `LogAspect` registra los
argumentos de los servicios y no deben aparecer contraseñas en el log.

## Pruebas

`./mvnw test` — usan H2 en memoria (`src/test/resources/application.properties`).

## Sesión, cookies y base de datos

Tres mecanismos distintos para guardar información. Cada uno responde a una pregunta diferente:

| | Sesión (`HttpSession`) | Cookie | Base de datos |
|---|---|---|---|
| **Dónde vive** | En el **servidor** (memoria) | En el **navegador** del usuario | En el **servidor** (MySQL), en disco |
| **Cuánto dura** | Hasta cerrar sesión o 30 min sin actividad | Lo que se defina (la de tema: 1 año) | Hasta que se borre |
| **Sobrevive a reiniciar el servidor** | No | Sí (está en el navegador) | Sí |
| **Quién puede alterarla** | Solo el servidor | El usuario puede editarla → **no es confiable** | Solo el servidor |
| **Sirve para** | "Quién está conectado ahora" | Preferencias simples de ese navegador | Datos del negocio: usuarios, aulas, reservas |
| **En este proyecto** | Nombre del usuario activo (`usuarioNombre`) | Tema visual (`tema`) y el id de sesión (`JSESSIONID`) | `usuarios`, `aulas`, `cursos`, `reservas` |

Cómo se conectan: la sesión **usa** una cookie. El servidor guarda los datos de la sesión y entrega al navegador solo
la cookie `JSESSIONID` con un identificador; en cada petición el navegador la devuelve y el servidor encuentra su sesión.

### Sesión (HttpSession)
- **Ingreso:** `LoginSuccessHandler` guarda el nombre con `session.setAttribute("usuarioNombre", ...)`.
- **Lectura:** `ModeloGlobalAdvice` lo recupera con `session.getAttribute(...)` y el menú lo muestra en todas las páginas.
- **Salida:** `CerrarSesionHandler` quita el atributo y ejecuta `session.invalidate()`; además se borra la cookie `JSESSIONID`.
- La sesión caduca tras 30 minutos sin actividad (`server.servlet.session.timeout`).

### Cookie de preferencias (tema visual)
- El botón **Tema oscuro / Tema claro** del menú envía `POST /preferencias/tema`.
- `PreferenciasController` → `PreferenciasService` crea la cookie `tema` (`HttpOnly`, `SameSite=Lax`, 1 año, `Secure` si hay HTTPS).
- En cada petición `ModeloGlobalAdvice` lee la cookie (`@CookieValue`) y, si vale `oscuro`, las páginas cargan `tema-oscuro.css`.
- La cookie **no se borra al cerrar sesión**: es una preferencia del navegador, no del acceso.
- Solo se aceptan los valores `claro` y `oscuro`; cualquier otro se descarta.

### Páginas de error
`templates/error/404.html` (página no encontrada), `templates/error/500.html` (error interno) y `templates/error.html`
(400, 403, 405 y demás). Todas muestran un mensaje claro, sin detalles técnicos, y el botón **Volver a la aplicación**.

