import pika
from .rabbit_config import get_rabbitmq_config


def create_rabbit_con_and_return_channel(conn: pika.BlockingConnection = None) -> pika.channel.Channel:
    if conn is None:
        conn = __create_connection()

    return __create_channel(conn)


def __create_connection() -> pika.BlockingConnection:
    rabbitConfig = get_rabbitmq_config()
    creds = pika.PlainCredentials(rabbitConfig.username, rabbitConfig.password)
    params = pika.ConnectionParameters(
        host=rabbitConfig.host,
        port=rabbitConfig.port,
        virtual_host=rabbitConfig.vhost,
        credentials=creds,
        heartbeat=30,
        blocked_connection_timeout=300,
    )
    return pika.BlockingConnection(params)

def __create_channel(conn: pika.BlockingConnection) -> pika.channel.Channel:
    rabbitConfig = get_rabbitmq_config()
    ch = conn.channel()
    # Topology gets declared from rabbitmq/definitions.json at startup → NO queue_declare / exchange_declare necessary
    ch.basic_qos(prefetch_count=rabbitConfig.prefetch_count)
    return ch

