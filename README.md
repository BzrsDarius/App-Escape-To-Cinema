# Escape To Cinema 🎬✨

**Tu Portal Personalizado al Cine de Culto, Clásicos de los 80 y la Atmósfera Neón de John Carpenter.**

---

## Tabla de Contenidos
1. [Descripción](#descripción)
2. [Características Principales](#características-principales)
3. [Estética y Concepto Visual](#estética-y-concepto-visual)
4. [Tecnologías Utilizadas](#tecnologías-utilizadas)
    - [Aplicación Android (Frontend)](#aplicación-android-frontend)
    - [Backend API (EscapeToCinemaApi)](#backend-api-escapetocinemaapi)
    - [Servicios en la Nube y APIs Externas](#servicios-en-la-nube-y-apis-externas)
5. [Estructura del Proyecto](#estructura-del-proyecto)
6. [Instalación y Ejecución (Desarrollo)](#instalación-y-ejecución-desarrollo)
    - [Prerrequisitos](#prerrequisitos)
    - [Aplicación Android](#aplicación-android)
    - [Backend API](#backend-api)
7. [Landing Page (Conceptual)](#landing-page-conceptual)
8. [Trabajo Futuro](#trabajo-futuro)
9. [Autor](#autor)
10. [Agradecimientos (Opcional)](#agradecimientos-opcional)
11. [Licencia (Opcional)](#licencia-opcional)

---

## Descripción

**Escape To Cinema** es una aplicación móvil Android nativa, desarrollada como un Trabajo de Fin de Ciclo (TFC) para el grado de Desarrollo de Aplicaciones Multiplataforma. Está diseñada para ser un destino digital integral y temático para aficionados al cine y series, con un fuerte enfoque en el cine de culto, los clásicos de los años 70 y 80, y una distintiva estética visual oscura con acentos de neón, inspirada en la obra cinematográfica de John Carpenter y la cultura retro-futurista.

El proyecto busca ofrecer una experiencia de usuario inmersiva que va más allá de un simple catálogo de películas, integrando funcionalidades de descubrimiento, gestión personalizada, interacción social básica (valoraciones y reseñas), información actualizada y entretenimiento temático.

## Características Principales

*   **Descubrimiento Cinematográfico:** Listas de películas populares, mejor valoradas, en cartelera y secciones temáticas curadas (ej. "Terror de los 80", "Joyas de John Carpenter").
*   **Búsqueda Avanzada:** Búsqueda por texto con filtros por género, año, valoración y ordenación.
*   **"Mi Lista" Personal:** Guarda y gestiona tus películas y series favoritas.
*   **Valoraciones y Reseñas:** Valora películas y escribe/lee reseñas de otros usuarios.
*   **Noticias de Cine:** Agregador de noticias de múltiples fuentes RSS relevantes.
*   **Trivia de Cine:** Un juego interactivo para poner a prueba tus conocimientos cinéfilos, con diferentes categorías y sistema de logros.
*   **Cines y Carteleras:** Encuentra cines cercanos y consulta sus horarios (integración con MovieGlu API).
*   **Línea de Tiempo del Cine:** Explora eventos e hitos importantes de la historia del cine.
*   **Chatbot con IA "Príncipe de las Tinieblas":** Un asistente virtual temático potenciado por Google Gemini API para recomendaciones, datos curiosos y conversación cinéfila con personalidad.
*   **Perfil de Usuario:** Visualiza tu "Mi Lista", logros desbloqueados, puntuaciones máximas de trivia y gestiona ajustes de la cuenta (cambio de contraseña, eliminación de cuenta, preferencia de país para "Dónde Ver").
*   **Estética Inmersiva:** Tema oscuro con acentos de neón vibrantes (Cyan, Magenta, Naranja) y tipografías personalizadas (Orbitron, Rajdhani).

## Estética y Concepto Visual

La identidad visual de "Escape To Cinema" es un pilar fundamental. Se inspira en:
*   La atmósfera oscura y de suspense de las películas de **John Carpenter**.
*   La estética **Neón de los 80**, el estilo **Synthwave** y **Outrun**.
*   La cultura de los **videoclubs** y las portadas de **VHS** de la época.
*   Elementos visuales del **Cyberpunk** ligero.

El objetivo es crear una interfaz que no solo sea funcional sino que también transporte al usuario a este universo temático.

## Tecnologías Utilizadas

### Aplicación Android (Frontend)
*   **Lenguaje:** Kotlin
*   **UI Toolkit:** Jetpack Compose (con Material Design 3)
*   **Arquitectura:** MVVM (Model-View-ViewModel)
*   **Navegación:** Jetpack Navigation Compose
*   **Asincronía:** Kotlin Coroutines y Flow
*   **Networking:** Retrofit 2, OkHttp 3
*   **Serialización JSON:** Kotlinx.serialization
*   **Carga de Imágenes:** Coil
*   **Paginación:** Jetpack Paging 3
*   **Parseo RSS:** `com.prof18.rssparser`
*   **Firebase:** Authentication (Cliente), Firestore (Cliente)
*   **Almacenamiento Local:** SharedPreferences

### Backend API (EscapeToCinemaApi)
*   **Lenguaje:** Java
*   **Framework:** Spring Boot (con Spring WebFlux)
*   **Despliegue:** Google Cloud Run (Contenedor Docker)
*   **Comunicación APIs Externas:** `WebClient`

### Servicios en la Nube y APIs Externas
*   **Firebase:** Authentication, Cloud Firestore
*   **Google Cloud Platform:** Google AI Studio (Gemini API)
*   **The Movie Database (TMDb) API:** (Consumida vía Backend)
*   **MovieGlu API:** (Consumida directamente desde la App)
*   **Feeds RSS:** (Múltiples fuentes)

## Estructura del Proyecto

El proyecto Android sigue una arquitectura MVVM con una clara separación de responsabilidades en paquetes:
*   `data`: Contiene modelos, repositorios, DTOs y fuentes de datos (red, local).
*   `ui`: Alberga los Composables (pantallas, componentes), ViewModels, UiStates y el tema.
*   `util`: Clases de utilidad y helpers.
*   `di`: Factories para la inyección de dependencias de ViewModels.

El proyecto Backend (Spring Boot) se estructura en:
*   `controller`: Endpoints de la API.
*   `service`: Lógica de negocio e interacción con APIs externas.
*   `dto`: Data Transfer Objects.
*   `config`: Clases de configuración.

## Instalación y Ejecución (Desarrollo)

### Prerrequisitos
*   Android Studio [Hedgehog]
*   JDK [17] (para el backend)
*   Maven o Gradle (para el backend)
*   IntelliJ IDEA o VS Code (para el backend, opcional)
*   Cuenta de Firebase configurada con Authentication y Firestore habilitados.
*   API Keys para TMDb, MovieGlu y Google Gemini (configuradas como se indica abajo).

### Aplicación Android
1.  Clona este repositorio.
2.  Abre el proyecto Android (la carpeta `/app` o la raíz si es un monorepo) con Android Studio.
3.  **Configuración de Firebase:**
    *   Añade tu archivo `google-services.json` (descargado de tu consola de Firebase) en el directorio `app/`.
4.  **Configuración de API Keys (MovieGlu):**
    *   Crea un archivo `local.properties` en la raíz del proyecto Android (si no existe).
    *   Añade tus claves de MovieGlu:
        ```properties
        MOVIEGLU_API_KEY="TU_API_KEY_MOVIEGLU"
        MOVIEGLU_AUTHORIZATION="TU_AUTHORIZATION_HEADER_MOVIEGLU" 
        ```
5.  **Configuración de la URL del Backend:**
    *   En la clase/objeto donde defines la URL base para Retrofit (`TmdbApiService`), asegúrate de que apunta a tu instancia desplegada del backend o a `http://localhost:8080` (o el puerto que uses) si ejecutas el backend localmente.
6.  Sincroniza el proyecto con los archivos Gradle.
7.  Ejecuta la aplicación en un emulador o dispositivo físico.

### Backend API
1.  Navega al directorio del proyecto backend.
2.  **Configuración de API Keys (TMDb, Gemini):**
    *   La aplicación Spring Boot está configurada para leer estas claves desde variables de entorno.
    *   Al ejecutar localmente, puedes configurarlas en tu IDE o pasarlas como argumentos de JVM:
        `-DTMDB_API_KEY=TU_KEY_TMDB -DGEMINI_API_KEY=TU_KEY_GEMINI`
    *   Para el despliegue en Cloud Run, estas se configuran como variables de entorno en el servicio.
3.  Construye y ejecuta el proyecto Spring Boot (ej. `mvn spring-boot:run` o desde tu IDE).
    *   Por defecto, se ejecutará en `http://localhost:8080`.

## Landing Page (Conceptual)
Se ha diseñado conceptualmente una landing page promocional para "Escape To Cinema", manteniendo la estética oscura y neón de la aplicación. Esta página incluiría:
*   Sección Hero con CTA.
*   Presentación de Características Clave.
*   Muestra de la Estética Visual de la App.
*   Introducción al Chatbot "Príncipe de las Tinieblas".
*   Formulario de Suscripción (pre-lanzamiento) o enlaces de descarga.
*(Ver la memoria del TFC para el diseño detallado y esqueleto HTML propuesto).*

## Trabajo Futuro
"Escape To Cinema" tiene un gran potencial de crecimiento. Algunas líneas futuras incluyen:
*   **Expansión a Series de TV:** Soporte completo para el seguimiento de series.
*   **Notificaciones Push Inteligentes:** Alertas personalizadas para estrenos, noticias, etc.
*   **IA del "Príncipe de las Tinieblas" 2.0:** Mejor contexto y capacidad de realizar acciones.
*   **Modo Invitado:** Permitir una vista previa de la app sin registro.
*   **Monetización Freemium:** Versión Pro con características avanzadas opcionales.
*   **Componente Social Mejorado:** Opciones para seguir usuarios, listas colaborativas.

## Autor
*   **Darío Bezares Jarabo**
    *   Estudiante de Desarrollo de Aplicaciones Multiplataforma.

---
