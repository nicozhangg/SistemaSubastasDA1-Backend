- Usuarios no registrados no ven precios ni pueden pujar

- Las categorías son temáticas de subasta (ej: obras de arte), no del ítem. La estructura es: subasta → temática → ítems que se subastan uno después del otro

- Reemplazar la barra de navegación superior por una navbar inferior con accesos rápidos.

- Agregar autenticación del DNI en el registro (o por lo menos carga de imágen).

- En la pantalla de ítem, agregar lógica de puja mínima (ej: no se puede pujar menos de +5% de la última puja). Mostrar directamente el número mínimo pujable como opción, junto a un input numérico personalizado

- El usuario sugiere el precio del artículo que sube, la empresa toma la decisión final. Cada “publicación” es una solicitud, no una carga directa

- Al tocar un ítem en subasta debe mostrarse: dónde está publicado, la póliza del seguro y la ubicación física del artículo

- Agregar pantallas de errores y warnings


SWAGGER/API

- El email debe ser obligatorio en el POST del paso 1 del registro
- Agregar lo del DNI al flujo de registro
- POST /subastas/{id}/conectar: el código de error actual (400) debe ser 409
- Implementar tiempo real de pujas con WebSocket