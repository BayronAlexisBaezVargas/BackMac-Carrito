#!/bin/sh
set -e

echo "Arrancando PostgreSQL en el puerto 5434..."
su-exec postgres pg_ctl start -D /var/lib/postgresql/data -w -o "-p 5434"

echo "Configurando la base de datos y usuario para Carrito..."
su-exec postgres psql -p 5434 -c "CREATE DATABASE carrito_db;" || true
su-exec postgres psql -p 5434 -c "CREATE USER springuser WITH PASSWORD 'springpassword';" || true
su-exec postgres psql -p 5434 -c "GRANT ALL PRIVILEGES ON DATABASE carrito_db TO springuser;" || true
su-exec postgres psql -p 5434 -d carrito_db -c "GRANT ALL ON SCHEMA public TO springuser;" || true

echo "Arrancando microservicio de Carrito..."
exec java -jar /app/app.jar
