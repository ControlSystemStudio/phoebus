#
# Author(s): Evan Smith, smithej@ornl.gov
#
import threading
import time
import random
import paho.mqtt.client as mqtt

topic = "random"
will_message = "random has disconnected"

client = mqtt.Client()

client.will_set(topic, will_message, 0, True)

client.connect("localhost", 1883, 30)

topic_numbers = range(50)

# Publish 50 random numbers.
def publishRandomNumbers():
    while(True):
        thread_name = threading.current_thread().name
        for i in topic_numbers:
            client.publish("{}/{}/number{}".format(topic, thread_name, i), random.random(), 0, False)
        time.sleep(1)

# Publish 50 numbers from 10 threads each.
for i in range(10):
    thread = threading.Thread(target=publishRandomNumbers)
    thread.setName("thread" + str(i))
    thread.start()
    

