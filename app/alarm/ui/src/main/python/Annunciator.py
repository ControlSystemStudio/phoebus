from confluent_kafka import Consumer, KafkaError
import threading
import Queue
import uuid
import subprocess
import argparse
import sys
import json

server = 'localhost:9092'
config = 'Accelerator'

argParser = argparse.ArgumentParser()

argParser.add_argument("threshold", type=int, help="The number of messages that the message queue can accumulate before a generic message, 'There are N new messages', is annunciated.")
argParser.add_argument("-server", "--server", help="The server name that the annunciator will listen to.")
argParser.add_argument("-config", "--config", help="The config name that the annunciator will listen to.")

args = argParser.parse_args()
# Threshold of message count that causes a generic notification
threshold = args.threshold
if args.server:
    server = args.server
if args.config:
    config = args.config

print("Annunciator started with server value: {}".format(server))
print("Annunciator started with config value: {}".format(config))

# Kafka spreads messages across groups so group.id should be unique so that every 
# alarm listener gets all of the messages.
c = Consumer({
    'bootstrap.servers': '{}'.format(server),
    'group.id' : 'Alarm-' + str(uuid.uuid4()),
    'default.topic.config' : {'auto.offset.reset':'largest'} # Start reading from the end of the topic.
    })

c.subscribe(['{}Talk'.format(config)])
print("Connected to topic {}Talk".format(config))

# Message queue and accompanying lock.
annunciationQueue = Queue.PriorityQueue()
queueLock = threading.Lock()

# Condition variable signifying that there are messages to annunciate.
annunciateCondition = threading.Condition()

# Should the annunciator run?
run = True

# Annunciator, runs in its own thread, acts as consumer to annunciationQueue.
class annunciatorThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.name = "Annunciator: Message Queue Consumer"
        # The thread should die when the script is killed.
        self.daemon = True

    def run(self):
        while (run):
            queueLock.acquire()
            size = annunciationQueue.qsize()
            
            if size > threshold:
                self.handleNAnnunciations()
            
            else:
                while not annunciationQueue.empty():
                    annunciation = annunciationQueue.get()
                    self.handleAnnunciation(annunciation)
            
            queueLock.release()
            
            annunciateCondition.acquire()
            annunciateCondition.wait()

    # Annunciates "There are N new messages"
    def handleNAnnunciations(self):
        flurry = 0
        while not annunciationQueue.empty():
             annunciation = annunciationQueue.get()
             standout = annunciation[1]
             if standout:
                 self.handleAnnunciation(annunciation)
             else:
                 flurry+=1
        ex = "echo \"There are {} new messages.\" | festival --tts".format(flurry)
        subprocess.call(ex, shell=True)
    
    # Annunciates message
    def handleAnnunciation(self, annunciation):
        toSay = annunciation[2]
        ex = "echo \"{}\" | festival --tts".format(toSay)
        subprocess.call(ex, shell=True)

# Add a message to the queue.                         
def enqueueMessage(message):
    threading._start_new_thread(annunciationProducer, (message,))

# Add the message to the queue in another thread. Notify all waiting consumer threads.
def annunciationProducer(message):
    annunciation = parseMessage(message)
    
    queueLock.acquire()
    annunciationQueue.put(annunciation)
    queueLock.release()
    
    annunciateCondition.acquire()
    annunciateCondition.notify_all()
    annunciateCondition.release()

# Parse the message and create the annunciation tuple (priority, standout, toSay)
def parseMessage(message):
    parsed_json = json.loads(message.value().decode('utf-8'))
    
    severity = parsed_json['severity']
    priority = getMessagePriority(severity)
    standout = parsed_json['standout']
    message  = parsed_json['talk']

    return ((priority, standout, message))

severities = ["UNDEFINED", "INVALID", "MAJOR", "MINOR", "UNDEFINED_ACK", "INVALID_ACK", "MAJOR_ACK" , "MINOR_ACK", "OK"]
# Determine the priority of the message based on the alarm severity.
def getMessagePriority(severity):   
    try:
        priority = severities.index(severity)
    except ValueError:
        return 10
    return priority

annunciator = annunciatorThread()
annunciator.start()

print("Started the annunciator.")
print("Beginning to poll for messages.")
while True:
    msg = c.poll()
    
    if msg is None:
        continue
    
    if msg.error():
        if msg.error().code() == KafkaError._PARTITION_EOF:
            continue
        else:
            print(msg.error())
    
    enqueueMessage(msg)
    