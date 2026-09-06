# ── Fase 1: Construcción ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Primero nos traemos la configuración de Maven para aprovechar la caché.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Descargamos las dependencias
RUN ./mvnw dependency:go-offline -q

# Ahora sí copiamos el código fuente y compilamos el JAR
COPY src src
RUN ./mvnw package -DskipTests -q

# ── Fase 2: Ejecución ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Instalamos PostgreSQL y su-exec que nos hará falta para levantar la base de datos local del contenedor
RUN apk add --no-cache postgresql postgresql-contrib su-exec

# Armamos los directorios necesarios y ajustamos los permisos para el usuario postgres
RUN mkdir -p /run/postgresql && chown -R postgres:postgres /run/postgresql
RUN mkdir -p /var/lib/postgresql/data && chown -R postgres:postgres /var/lib/postgresql/data

# Inicializamos el clúster de la base de datos
USER postgres
RUN initdb -D /var/lib/postgresql/data
USER root

WORKDIR /app

# Nos traemos el script de arranque y lo hacemos ejecutable
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Y por último, copiamos el JAR de Carrito que acabamos de construir
COPY --from=builder /app/target/carrito-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto del MS (8082) y el de Postgres interno
EXPOSE 8082 5432

ENTRYPOINT ["/entrypoint.sh"]
