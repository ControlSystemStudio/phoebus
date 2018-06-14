from confluent_kafka import Producer
from time import sleep
from rhn.nonblocking import callback

p = Producer({'bootstrap.servers': 'localhost:9092'})

topic = 'AcceleratorTalk'

def delivery_report(err, msg):
    """ Called once for each message produced to indicate delivery result.
        Triggered by poll() or flush(). """
    if err is not None:
        print('Message delivery failed: {}'.format(err))
    else:
        print('Message delivered to {} [{}]'.format(msg.topic(), msg.partition()))

# Demo the ability of the annunciator to receive a message.
p.poll(0)
p.produce(topic, "We are out of coolaid".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()

sleep(3)

# Demo the ability of the annunciator to receive a message ignoring severity. 
p.poll(0)
p.produce(topic, "*We are out of coolaid".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()

sleep(3)

# Demo the ability of the annunciator to receive a string of messages, sort them on priority, and annunciate them.
p.poll(0)
p.produce(topic, "We are out of coolaid".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of barbeque sauce".encode('utf-8'), "MINOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of potato salad".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()

sleep(11)

# Demo the ability of the annunciator to receive a string of messages greater than the queue threshold.
p.poll(0)
p.produce(topic, "We are out of coolaid".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of barbeque sauce".encode('utf-8'), "MINOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of potato salad".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of barbeque sauce".encode('utf-8'), "MINOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of potato salad".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of potato salad".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of barbeque sauce".encode('utf-8'), "MINOR".encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, "We are out of potato salad".encode('utf-8'), "MAJOR".encode('utf-8'), callback=delivery_report)
p.flush()
