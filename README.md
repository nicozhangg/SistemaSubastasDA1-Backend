# BidUp - Sistema de Subastas Móvil

**Desarrollo de Aplicaciones I - 1C Lunes 2026**  
**Grupo 5:**  
Nicolas Hernández  
Ivo Guido Biscardi  
Nicolás Zhang  
Puleio Santiago  
Tobías Hernández  

---

## Backend — Spring Boot

API REST con **Spring Boot 3.3.4**, **Java 17**, **Spring Security + JWT**, **WebSocket (STOMP)** y base de datos **H2** en desarrollo / **MySQL** en producción.

### Requisitos

- Java 17+
- Maven 3.6+

### Correr en desarrollo

```bash
mvn spring-boot:run
```

La API levanta en **http://localhost:8080**.  
La documentación interactiva (Swagger UI) queda disponible en **http://localhost:8080/swagger-ui/index.html**.

### Compilar y ejecutar el JAR

```bash
mvn clean package
java -jar target/subastas-api-0.0.1-SNAPSHOT.jar
```

### Correr los tests

```bash
mvn test
```

| Carpeta | Descripción |
|---|---|
| `src/main/java/com/subastas/controller/` | Controladores REST |
| `src/main/java/com/subastas/service/` | Lógica de negocio |
| `src/main/java/com/subastas/model/` | Entidades, DTOs y enums |
| `src/main/java/com/subastas/repository/` | Repositorios JPA |
| `src/main/java/com/subastas/security/` | Filtro JWT y configuración de seguridad |
| `src/main/java/com/subastas/config/` | Configuración de WebSocket, OpenAPI y datos iniciales |
| `subastas-api.yaml` | Contrato OpenAPI del backend |

---

## Descripción del Proyecto

La empresa opera subastas presenciales con un sistema local que no expone interfaz móvil. Los postores no pueden participar de forma remota, gestionar sus medios de pago ni solicitar la consignación de bienes desde un dispositivo. BidUp resuelve esto mediante una aplicación móvil que consume y actualiza ese sistema a través de una API REST propia, integrando el flujo completo de subasta —desde el registro del postor hasta la resolución de la compra.
 
El modelo de negocio implementado es **subasta dinámica ascendente**: precio base, ofertas visibles y competitivas, ganador por oferta máxima. Las reglas de puja tienen límites calculados sobre la última oferta y el valor base del bien (mínimo +1%, máximo +20%), con excepciones para categorías *oro* y *platino*.
 
---
 
## Arquitectura y Stack
 
La API sigue una arquitectura **modular por dominio**, documentada en Swagger (`subastas-api.yaml`). Los módulos son:
 
- **Auth** - Registro en dos pasos + login/logout con JWT Bearer
- **Usuarios** - Perfil, categoría y estado de cuenta
- **Medios de Pago** - Alta/baja de tarjetas, cuentas bancarias y cheques certificados
- **Subastas** - Listado con filtros (estado, categoría, moneda), detalle y gestión de sesión
- **Pujas** - Estado actual del ítem, validación de oferta y registro de historial
- **Catálogo** - Ítems con visibilidad diferencial según autenticación
- **Compras** - Detalle de la compra ganada: monto ofertado, comisiones, costo de envío, total, moneda, medio de pago y estado del pago
- **Consignación** - Solicitud de subasta de bienes propios, aceptación de condiciones, seguro y ubicación en depósito
- **Métricas** - Historial de participaciones, estadísticas y gestión de multas
 
La seguridad se implementa mediante **JWT en el header `Authorization`**. Los endpoints públicos (catálogo sin precio base, listado de subastas) son accesibles sin token; los operativos requieren autenticación y, en varios casos, un medio de pago verificado.
 
---
 
## Flujos Principales
 
**Registro**
El onboarding opera en tres pasos: (1) datos personales, (2) tipo de documento, número, país, domicilio y código postal + aceptación de términos, (3) carga opcional de medio de pago ("Más tarde" disponible). El registro envía la cuenta a estado *pendiente* de aprobación manual por la empresa, representado en la app con una pantalla de espera. Una vez aprobada, la empresa asigna una categoría (*común, especial, plata, oro, platino*) que determina a qué subastas puede acceder el usuario. La activación se realiza por token enviado al email registrado.
 
**Acceso como invitado**
La pantalla de acceso da la opción *"Ingresar como invitado"*, que permite navegar el catálogo y el listado de subastas sin autenticación. El precio base de los ítems no es visible en este modo. Al intentar realizar una acción que requiere cuenta (como pujar), la app presenta un *login wall* que solicita iniciar sesión o registrarse.
 
**Participación en subasta**
Al conectarse a una subasta activa, la API devuelve un `sesion_id`, el estado del ítem en curso y una `streaming_url` para seguimiento en tiempo real. El sistema bloquea la conexión simultánea a más de una subasta. Antes de ejecutar una puja, la app presenta un modal de confirmación. Cada puja requiere confirmación del backend antes de habilitar la siguiente, garantizando consistencia en el orden de transacciones.
 
**Resolución de compra**
Al ganar una subasta, el módulo de Compras devuelve el desglose completo de la operación: monto ofertado, comisiones, costo de envío, total, moneda, medio de pago utilizado y estado del pago. Este detalle conecta el proceso de puja con la etapa de pago y logística.
 
**Consignación**
Un usuario puede solicitar incluir un bien propio en subasta: carga de datos, al menos 6 fotos y declaración de titularidad. La empresa inspecciona el bien, propone valor base y comisiones. El usuario acepta o rechaza; en caso de rechazo, la devolución corre a cargo del consignante. BidUp permite al propietario consultar la ubicación del bien en depósito y los datos de la póliza de seguro contratada.
 
**Penalizaciones**
Si un postor ganador no puede pagar, recibe una multa del 10% sobre el monto ofertado y queda bloqueado hasta abonarla y presentar los fondos en 72 horas. El módulo de Métricas expone el listado de multas pendientes y el endpoint de pago correspondiente.
 
---
 
## Gestión de Visibilidad y Acceso
 
| Recurso | Sin sesión / Invitado | Sesión (cualquier categoría) | Sesión + medio de pago verificado |
|---|---|---|---|
| Catálogo | Visible (sin precio base) | Visible (con precio base) | — |
| Listado de subastas | Visible | Visible | — |
| Conectarse a subasta | ✗ | ✓ (si categoría compatible) | Requerido para pujar |
| Pujar | ✗ | ✗ | ✓ |
| Consignación | ✗ | ✓ | — |
 
---
 
## Diseño de Interfaz
 
El diseño está maquetado en **Figma** con wireframes en alta definición, paleta de colores definida, ícono de aplicación y pantalla de splash.
 
🔗 [Ver diseño en Figma](https://www.figma.com/design/sSGcSmOo9u9k3rSXhj6HtO/Des_apps1_Subastas?m=auto&t=0MzEp0q036gkvFO4-6)
 
Pantallas incluidas en el diseño:
 
- Splash / Acceso (con opción de ingreso como invitado) / Login / Recupero de contraseña
- Registro en 3 pasos + pantalla de espera de aprobación
- Home diferenciado (invitado vs. logueado) con navegación por categorías
- Login wall para acciones que requieren cuenta
- Detalle de subasta con historial de pujas en vivo, puja rápida y puja personalizada + modal de confirmación
- *Mis Pujas* con filtros: Todas / Perdidas / Pendientes / Ganadas
- *Mis Subastas* con filtros: Todas / Canceladas / Finalizadas
- Gestión de perfil: configuración, direcciones (alta y edición), medios de pago (tarjeta y cheque)
- Chat entre usuarios
- Subir artículo para consignación + pantalla de confirmación de artículo subido
 
---
 
## API — Documentación Swagger
 
La especificación completa del contrato REST está en `subastas-api.yaml` (raíz del repositorio). También puede cargarse en Swagger Editor:
 
🔗 [Ver en Swagger Editor](https://editor.swagger.io/?_gl=1*7ctjd2*_gcl_au*NjcwMjEzMDA2LjE3NzYxMTU1OTQ)
 
Recomendamos usar el archivo directamente en caso de que el link no resuelva correctamente.
