FROM rabbitmq:4-management

# Copy config and definitions into the container
COPY config/rabbit.conf /etc/rabbitmq/rabbitmq.conf
COPY config/definitions.json /etc/rabbitmq/definitions.json

# Set environment variables
ENV RABBITMQ_CONFIG_FILE=/etc/rabbitmq/rabbitmq
ENV RABBITMQ_DEFAULT_USER=admin
ENV RABBITMQ_DEFAULT_PASS=admin

# Expose ports
EXPOSE 5672 15672
