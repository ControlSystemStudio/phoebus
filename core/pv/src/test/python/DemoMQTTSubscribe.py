#
# Author(s): Evan Smith, smithej@ornl.gov
#

import paho.mqtt.client as mqtt

topic = "DemoMQTT"
will_message = "DemoMQTT has disconnected."

def on_client_connect(client, userdata, flags, rc):
    print "Connected with result code " + str(rc)
    
    client.subscribe(topic)

def on_message_recieved(client, userdata, message):
    print message.topic + " : " + str(message.payload)

client = mqtt.Client()

client.on_connect = on_client_connect
client.on_message = on_message_recieved

client.will_set(topic, will_message, 0, True)

client.connect("localhost", 1883, 30)

client.loop_forever()

