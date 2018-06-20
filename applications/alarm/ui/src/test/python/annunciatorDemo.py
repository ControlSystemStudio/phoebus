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


key ="/test"
coolaid =     '{"severity":"MAJOR", "standout":false, "talk":"MAJOR Alarm: We are out of purple cool aid."}'
barbeque =    '{"severity":"MINOR", "standout":false, "talk":"MINOR Alarm: We are out of barbecue sauce."}'
tater_salad = '{"severity":"MAJOR", "standout":false, "talk":"MAJOR Alarm: We are out of potato salad."}'
dont_ignore = '{"severity":"OK",    "standout":true,  "talk":"This message will not be ignored."}'

print("This demo expects there to be a kafka topic 'AcceleratorTalk' and for the annunciator threshold to be set at 4.")

# Demo the ability of the annunciator to receive a message.

p.poll(0)
p.produce(topic, coolaid.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()

sleep(3)

# Demo the ability of the annunciator to receive a message ignoring severity. 
p.poll(0)
p.produce(topic, coolaid.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()

sleep(3)

# Demo the ability of the annunciator to receive a string of messages, sort them on priority, and annunciate them.
p.poll(0)
p.produce(topic, coolaid.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, barbeque.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, tater_salad.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()

sleep(11)

# Demo the ability of the annunciator to receive a string of messages greater than the queue threshold.
# The annunciator will then annunciate a message stating "There are N new messages". N in this case should be 8.
p.poll(0)
p.produce(topic, coolaid.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, barbeque.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, tater_salad.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, barbeque.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, tater_salad.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, tater_salad.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, barbeque.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, tater_salad.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
p.produce(topic, dont_ignore.encode('utf-8'), key.encode('utf-8'), callback=delivery_report)
p.flush()
