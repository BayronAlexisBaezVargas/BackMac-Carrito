# Documentación Técnica: Microservicio de Carrito (ms-carrito)

## Descripción General
El microservicio `ms-carrito` es el componente responsable de gestionar la lógica de negocio asociada a los carritos de compra de los usuarios. Interactúa directamente con una base de datos PostgreSQL independiente para persistir el estado del carrito y se comunica de forma síncrona con el microservicio de catálogo (`ms-productos`) para validar existencias y precios mediante Spring Cloud OpenFeign.

## Arquitectura y Tecnologías
- **Lenguaje:** Java 21
- **Framework:** Spring Boot (v4.1.1)
- **Persistencia:** Spring Data JPA, PostgreSQL 18
- **Comunicación Inter-servicios:** Spring Cloud OpenFeign
- **Seguridad:** Decodificación manual de tokens JWT (compatible con Azure AD)
- **Contenerización:** Docker

## Requisitos Previos
1. **Java 21** instalado en el entorno local (para ejecución manual).
2. **Docker** instalado y en ejecución (para despliegue contenerizado).
3. **Microservicio de Productos (`ms-productos`)** activo y en ejecución, ya que el servicio de carrito depende de este para la validación del catálogo.

## Configuración de Red y Puertos
Por defecto, el servicio está configurado para operar con los siguientes parámetros en el archivo `application.yaml`:
- **Puerto del Servidor Spring Boot:** 8082
- **Puerto de PostgreSQL Integrado (Docker):** 5434
- **Base de Datos:** `carrito_db` (Credenciales por defecto: `springuser` / `springpassword`)

## Instrucciones de Ejecución

### Opción 1: Ejecución Contenerizada (Recomendada)
El proyecto incluye un `Dockerfile` y un script de inicialización (`entrypoint.sh`) que despliegan simultáneamente la aplicación y su propia instancia de base de datos aislada.

Para compilar y ejecutar el servicio, ejecute los siguientes comandos en la terminal desde el directorio raíz del microservicio:

```bash
docker build -t ms-carrito .
docker run -d --name ms-carrito --network host ms-carrito
```
*Nota: El parámetro `--network host` es estrictamente necesario para garantizar que el servicio pueda resolver correctamente la comunicación con `ms-productos` apuntando a la interfaz de loopback (localhost).*

### Opción 2: Ejecución Local (Maven)
Si dispone de una instancia de PostgreSQL ejecutándose localmente en el puerto 5434 con la base de datos `carrito_db` creada, puede iniciar el servicio en modo desarrollo:

```bash
./mvnw spring-boot:run
```

## Especificación de la API (Endpoints)

Todas las peticiones requieren que se incluya un token JWT válido en los encabezados HTTP para identificar al usuario. El microservicio decodificará la estructura Base64 del token para extraer el identificador único del cliente.

**Encabezado requerido:**
```http
Authorization: Bearer <token_jwt>
```

---

### 1. Consultar el Carrito
Recupera el estado actual del carrito asociado al usuario autenticado. Si el usuario no posee un carrito activo, el sistema creará e inicializará uno automáticamente.

- **Método:** `GET`
- **Ruta:** `/api/carrito`
- **Respuestas Esperadas:**
  - `200 OK`: Retorna el objeto del carrito con su lista de ítems.
  - `401 Unauthorized`: Token ausente o malformado.

### 2. Agregar o Sumar un Producto
Añade un nuevo producto al carrito o incrementa la cantidad si el producto ya se encontraba registrado. El servicio validará contra `ms-productos` que el inventario sea suficiente.

- **Método:** `POST`
- **Ruta:** `/api/carrito/items`
- **Cuerpo de la Petición (JSON):**
  ```json
  {
    "productoId": "uuid-del-producto",
    "cantidad": 2
  }
  ```
- **Respuestas Esperadas:**
  - `200 OK`: Producto agregado satisfactoriamente.
  - `400 Bad Request`: Inventario insuficiente.
  - `404 Not Found`: El ID del producto no existe en el catálogo.

### 3. Actualizar Cantidad Exacta
Modifica la cantidad absoluta de un producto específico en el carrito. Si la cantidad enviada es 0 o negativa, el producto será removido del carrito.

- **Método:** `PUT`
- **Ruta:** `/api/carrito/items/{productoId}?cantidad={numero}`
- **Respuestas Esperadas:**
  - `200 OK`: Cantidad actualizada correctamente.
  - `400 Bad Request`: Inventario insuficiente para cubrir la nueva cantidad.
  - `404 Not Found`: El producto no se encuentra actualmente en el carrito.

### 4. Remover un Producto Específico
Elimina un producto del carrito en su totalidad, independientemente de la cantidad almacenada.

- **Método:** `DELETE`
- **Ruta:** `/api/carrito/items/{productoId}`
- **Respuestas Esperadas:**
  - `200 OK`: Ítem removido exitosamente.
  - `404 Not Found`: El producto no existe en el carrito del usuario.

### 5. Vaciar el Carrito
Elimina todos los ítems almacenados en el carrito del usuario, reiniciando su estado. Esta operación es recomendada tras la confirmación de una orden de compra o *checkout*.

- **Método:** `DELETE`
- **Ruta:** `/api/carrito`
- **Respuestas Esperadas:**
  - `204 No Content`: El carrito ha sido vaciado exitosamente.
