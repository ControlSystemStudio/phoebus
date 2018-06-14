from confluent_kafka import Consumer, KafkaError
import threading
import Queue
import uuid
import subprocess
import sys
import argparse

server = 'localhost:9092'
config = 'Acclerator'

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
    'default.topic.config' : {}
    })

c.subscribe(['{}Talk'.format(config)])
print("Connected to topic {}Talk".format(config))

# Message queue and accompanying lock.
queueLock = threading.Lock()
messageQueue = Queue.PriorityQueue()

# Condition variable signifying that there are messages to annunciate.
annunciateCondition = threading.Condition()

# Boolean value, should the annunciator run?
run = True

# Annunciator, runs in its own thread, acts as consumer to messageQueue.
class annunciatorThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.name = "Annunciator: Message Queue Consumer"
        # The thread should die when the script is killed.
        self.daemon = True

    def run(self):
        while (run):
            queueLock.acquire()
            size = messageQueue.qsize()
            
            if size > threshold:
                messageQueue.clear()
                handleNMessages(size)
            
            else:
                while not messageQueue.empty():
                    messageWithPriority = messageQueue.get()
                    handleMessage(messageWithPriority[1])
            
            queueLock.release()
            
            annunciateCondition.acquire()
            annunciateCondition.wait()

    # Annunciates "There are N new messages"
    def handleNMessages(N):
        ex = "echo \"There are {} new messages.\" | festival --tts".format(size)
        subprocess.call(ex, shell=True)
    
    # Annunciates message
    def handleMessage(message):
        toSay = message.value().decode('utf-8')
        
        if not toSay.startswith("*"):
            toSay = message.key().decode('utf-8') + 'Alarm: ' + toSay
        
        ex = "echo \"{}\" | festival --tts".format(Str)
        subprocess.call(ex, shell=True)

# Add a message to the queue.                         
def enqueueMessage(message):
    threading._start_new_thread(messageProducer, (message,))

# Add the message to the queue in another thread. Notify all waiting consumer threads.
def messageProducer(message):
    messagePriority = getMessagePriority(message)
    queueLock.acquire()
    messageQueue.put((priority, message))
    annunciateCondition.acquire()
    annunciateCondition.notify_all()
    annunciateCondition.release()
    queueLock.release()

severites = ["OK",  "MINOR_ACK",  "MAJOR_ACK", "INVALID_ACK", "UNDEFINED_ACK", "MINOR", "MAJOR", "INVALID", "UNDEFINED"]
# Determine the priority of the message based on the alarm severity.
def getMessagePriority(message):
    severity = message.key().decode('utf-8')
    try:
        priority = severities.index(severity)
    except ValueError:
        return 0
    return priority

annunciator = annunciatorThread()
annunciator.start()
print("Started the annunciator.")
print("Beginning to poll for messages.")
while True:
    msg = c.poll(sys.float_info.max)
    
    if msg is None:
        continue
    
    if msg.error():
        if msg.error().code() == KafkaError._PARTITION_EOF:
            continue
        else:
            print(msg.error())
            break
    
    enqueueMessage(msg)
    