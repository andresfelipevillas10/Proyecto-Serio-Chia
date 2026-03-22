# 🚌 App Conductores - Alcaldía de Chía

Este documento sirve como guía informativa para entender la estructura actual del proyecto y mantener la coherencia en el desarrollo. La arquitectura se basa en la **modularización** y el aprovechamiento de recursos nativos de Android.

---

## 🛠️ Stack Tecnológico
* **Lenguaje:** Kotlin (100% Nativo).
* **Diseño:** XML con Material Design Components.
* **Backend:** Firebase (Auth + Realtime Database).

---

## 🏗️ Organización de los Layouts (Vistas)
Para mantener el código limpio y organizado, estamos usando una arquitectura de **Componentes Reutilizables**. Esto significa que las pantallas no están en un solo archivo, sino que se arman como piezas de un rompecabezas.

### Ubicación de archivos clave (`res/layout/`):
* **`layout_header.xml`**: Encabezado con perfil de usuario.
* **`layout_network_status.xml`**: Indicador de conexión a la central.
* **`item_route_card.xml`**: Molde visual para las tarjetas de rutas en las listas.
* **Uso de `<include>`**: Estos archivos se insertan en las actividades principales (`Activity`) para evitar repetir código innecesariamente.

---

## 🗺️ Estructura de Clases y Lógica
Para que todos estemos en la misma página con el código fuente y las reglas de negocio, así es como están organizadas nuestras clases principales:

* **`Login.kt`, `Register.kt` y el modelo `User.kt`:** Son **exactamente las mismas que las del profe**. Mantenemos su estructura y estándar intactos para asegurar la compatibilidad con lo evaluado en clase.
* **`ForgotPasswordActivity.kt`:** Es una pantalla **nueva** que agregamos para la recuperación de cuentas, pero está construida utilizando exactamente la misma plantilla y lógica de Firebase enseñada por el profe.
* **`HomeRutasActivity.kt`:** Nuestro Dashboard principal. Aquí es donde se ensamblan los componentes modulares y sirve como base principal de navegación hacia las futuras pantallas de la app.

---

## 🎨 Gestión de Recursos (`res/values/`)
Para que la app sea fácil de mantener y escalar, todos los valores están centralizados:

1.  **Colores (`colors.xml`)**: Aquí se definen los tonos oficiales (Azul Chía, Verdes de estado, etc.). No uses códigos Hexadecimales directos en las vistas; llama al color por su nombre.
2.  **Textos (`strings.xml`)**: Todas las etiquetas y mensajes deben ir aquí. Esto facilita cambios rápidos en el futuro y posibles traducciones.
3.  **Vectores**: Usamos archivos XML para los íconos. Son ligeros y no se pixelan. Por favor, **evita subir imágenes pesadas** (.png/.jpg) para íconos o botones.

---

## 🔥 Conectividad con Firebase
El proyecto ya cuenta con el archivo `google-services.json` configurado.
* **Auth:** Gestiona el flujo de Login, Registro y Recuperación de Contraseña.
* **Realtime DB:** Almacena los datos de rutas y usuarios de forma dinámica.

---

## 💡 Recomendaciones del Equipo
* **Sync antes de trabajar:** Realiza un `git pull` cada vez que inicies sesión para evitar conflictos masivos en los archivos XML o clases.
* **Ciclo de vida:** Ten en cuenta que, al girar la pantalla, las actividades se reinician. Más adelante trabajaremos en mejorar la persistencia del estado.
* **Consistencia:** Si vas a crear una nueva funcionalidad, intenta que el diseño visual siga la línea de los componentes que ya están creados y respeta la lógica base dejada por el profe.