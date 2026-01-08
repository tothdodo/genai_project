FROM postgres:16

# Copy init scripts into the container (00-drop.sql runs first due to naming)
COPY config/00-drop.sql /docker-entrypoint-initdb.d/00-drop.sql
COPY config/init.sql /docker-entrypoint-initdb.d/init.sql

#working now watch out with the persistence
# Expose ports
EXPOSE 5432
