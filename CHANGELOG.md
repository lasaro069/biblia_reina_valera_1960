# Changelog - Biblia Reina Valera 1960 Devocional

## [1.1.0] - 2024-05-21
### Added
- **Navegación Táctil:** Implementación de gestos de deslizamiento (swipe) entre capítulos.
- **Búsqueda Avanzada:** Filtros por Antiguo Testamento, Nuevo Testamento y Libro Actual con contador de concordancias.
- **Persistencia de Ajustes:** La configuración de lectura (fuente, tamaño, tema, negrita) se guarda automáticamente.
- **Selector de Capítulos:** Botón de hamburguesa con navegación rápida entre libros y capítulos.
- **Nuevas Fuentes:** Opciones para cambiar entre Serif, SansSerif y Monospace.
- **Tema Rosa:** Nueva paleta de colores para personalización de lectura.
- **Sección de Créditos:** Pantalla dedicada con información del desarrollador y contacto directo vía WhatsApp.
- **Versículo del Día Dinámico:** Cambio automático cada día a las 07:00 AM (Hora Colombia).
- **Integración de Assets:** La App ahora utiliza archivos JSON locales para todos los libros y versículos.

### Changed
- **Iconografía:** Aumento de tamaño en iconos de navegación y tarjetas de inicio para mejor accesibilidad.
- **Optimización de Rendimiento:** La búsqueda ahora se realiza en hilos secundarios para evitar bloqueos de la App.
- **Temas Visuales:** Ajustes en los colores de los temas Pergamino y Sepia para mayor distinción.

### Fixed
- **XML Syntax:** Corrección de errores de declaración XML en iconos y recursos.
- **Text Formatting:** Limpieza de símbolos de salto de línea `/n` en los textos bíblicos.
- **Gradle Version:** Actualización a Gradle 9.3.1 para compatibilidad de compilación.
- **Typos:** Corrección de errores de redacción en diversos pasajes predefinidos.

---
## [1.0.0] - Versión Inicial
- Lanzamiento base con lectura, notas y temas básicos.
