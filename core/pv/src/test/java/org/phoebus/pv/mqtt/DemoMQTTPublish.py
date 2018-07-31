#
# Author(s): Evan Smith, smithej@ornl.gov
#

import psutil
import threading
import paho.mqtt.client as mqtt

topic = "1es/cpuPercent"
will_message = "1es/cpuPercent has disconnected"

client = mqtt.Client()

client.will_set(topic, will_message, 0, True)

client.connect("localhost", 1883, 30)

def publishCpuPercent():
    while(True):
        client.publish(topic, psutil.cpu_percent(interval=1), 0, False)

thread = threading.Thread(target=publishCpuPercent())
thread.setName("CPU PERCENT THREAD")
thread.start()