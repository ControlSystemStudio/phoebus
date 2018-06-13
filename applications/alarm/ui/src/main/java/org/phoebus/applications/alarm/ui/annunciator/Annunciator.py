from confluent_kafka import Consumer, KafkaError
import uuid

c = Consumer({
    'bootstrap.servers': 'localhost:9092',
    'group.id' : 'Alarm-' + str(uuid.uuid4()),
    'default.topic.config' : {}
    })

c.subscribe(['AcceleratorTalk'])

while True:
    msg = c.poll(10)
    
    if msg is None:
        continue
    
    if msg.error():
        if msg.error().code() == KafkaError._PARTITION_EOF:
            continue
        else:
            print(msg.error())
            break
    
    print('Recieved message: {}'.format(msg.value().decode('utf-8')))