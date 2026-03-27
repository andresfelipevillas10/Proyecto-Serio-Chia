# 🚌 Zenda - App de Navegación para Conductores (Alcaldía de Chía)

Zenda es una solución tecnológica integral diseñada para optimizar y asegurar las rutas de transporte en el municipio de Chía. Esta aplicación permite a los conductores gestionar sus recorridos, realizar seguimiento en tiempo real y reportar incidencias, todo bajo una interfaz moderna y eficiente.

---

## 🛠️ Stack Tecnológico
* **Lenguaje:** Kotlin (100% Nativo).
* **Arquitectura:** Basada en actividades modulares y componentes reutilizables.
* **Diseño:** XML con Material Design 3 y Tailwind-inspired UI.
* **Backend:** Firebase (Authentication + Realtime Database).
* **Mapas:** Google Maps SDK para Android.
* **Geolocalización:** Google Play Services Location (FusedLocationProviderClient).

---

## 🚀 Funcionalidades Principales

### 🔐 Seguridad y Acceso
* **Autenticación Robusta:** Flujo completo de registro, inicio de sesión y recuperación de contraseña con validaciones en tiempo real.
* **Perfil de Conductor:** Datos personalizados y gestión de sesión segura.

### 🏠 Dashboard Inteligente (Home)
* **Vista de Jornada:** Acceso rápido a la creación de nuevas rutas y reporte de incidentes.
* **Ruta Activa:** Visualización dinámica del progreso de la ruta actual (solo se muestra cuando hay una jornada en curso).
* **Estadísticas Rápidas:** Resumen de viajes y logros del conductor.

### 🗺️ Gestión de Rutas
* **Creador de Rutas:** Proceso simplificado en dos pasos para definir el nombre, descripción, horario y radio de detección.
* **Configurador de Puntos (Mapa):** Interfaz interactiva para añadir, editar y ordenar puntos de control (Origen, Marcas, Fin) directamente sobre Google Maps.
* **Lista de Rutas:** Gestión centralizada de todas las rutas creadas, permitiendo iniciar, editar o eliminar recorridos.

### 📍 Navegación en Tiempo Real
* **Modo Conducción:** Mapa optimizado con seguimiento GPS de alta precisión.
* **Detección Automática de Paradas:** El sistema detecta la llegada a los puntos de control mediante geofencing circular (radio configurable).
* **Velocímetro Digital:** Visualización de la velocidad actual en km/h.
* **Tracking en Vivo:** Sincronización constante de la ubicación del conductor con la base de datos central para monitoreo externo.
* **Resumen de Recorrido:** Al finalizar, se genera un reporte detallado de los tiempos y paradas realizadas.
* **Persistencia de Estado:** Si el conductor cierra la app accidentalmente, el dashboard permite retomar la navegación exactamente desde el último punto de control alcanzado.

---

## 🏗️ Estructura del Proyecto

### 📁 Archivos Clave de Lógica (`app/src/main/java/...`)
* **`HomeRutasActivity.kt`**: Corazón de la app, gestiona el estado global del conductor.
* **`ListaRutas.kt`**: Punto de entrada para la gestión de inventario de rutas.
* **`CrearRuta.kt` & `ConfigurarPuntosRuta.kt`**: El "Wizard" de creación de rutas.
* **`NavegacionActivaActivity.kt`**: Implementación del motor de navegación y tracking GPS.
* **`PreRecorridoActivity.kt`**: Vista previa y preparación antes de iniciar una jornada.

### 🎨 Recursos y Diseño (`app/src/main/res/`)
* **`layout/`**: Vistas modulares que utilizan `<include>` para componentes comunes (headers, status bars).
* **`values/strings.xml`**: Centralización de todos los textos para facilitar la mantenibilidad y localización.
* **`values/colors.xml`**: Definición de la paleta de colores institucional (Zenda Green, Accent Yellow, etc.).
* **`menu/bottom_nav_menu.xml`**: Configuración de la navegación principal.

---

## 🛠️ Estándares de Desarrollo
1. **Navegación Fluida:** Uso de `FLAG_ACTIVITY_REORDER_TO_FRONT` y transiciones instantáneas para una experiencia de usuario sin saltos.
2. **Modularización:** Lógica de UI (`initViews`), Navegación (`setupBottomNav`) y Datos (`setupFirebaseListeners`) separada en métodos dedicados.
3. **Cero Hardcoding:** Todos los strings y colores deben referenciarse desde los archivos de recursos.
4. **Validación Preventiva:** Verificación de datos en el cliente antes de cualquier interacción con Firebase.

---

## 💡 Instalación y Configuración
1. Clonar el repositorio.
2. Asegurarse de tener el archivo `google-services.json` en el directorio `app/`.
3. Abrir con Android Studio (Ladybug o superior recomendado).
4. Sincronizar Gradle y ejecutar en un dispositivo físico o emulador con Google Play Services.

---
**ADMINISTRACIÓN MUNICIPAL DE CHÍA • 2026**
"Tu ruta segura comienza aquí."
