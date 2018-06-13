from confluent_kafka import Consumer, KafkaError
import threading
import Queue
import uuid
import subprocess

# TODO Implement server and config name being taken from the command line.

# Kafka spreads messages across groups so group.id should be unique so that every 
# alarm listener gets all of the messages.
c = Consumer({
    'bootstrap.servers': 'localhost:9092',
    'group.id' : 'Alarm-' + str(uuid.uuid4()),
    'default.topic.config' : {}
    })

c.subscribe(['AcceleratorTalk'])

# Message queue and accompanying lock.
queueLock = threading.Lock()
messageQueue = Queue.Queue()

# Condition variable signifying that there are messages to annunciate.
annunciateCondition = threading.Condition()

# Boolean value, should the annunciator run?
run = True

# Threshold of message count that causes a generic notification
threshold = 3

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
                ex = "echo \"There are {} new messages.\" | festival --tts".format(size)
                subprocess.call(ex, shell=True)
            else:
                while not messageQueue.empty():
                    message = messageQueue.get()
                    Str = message.key().decode('utf-8') + ' Alarm: ' + message.value().decode('utf-8')
                    ex = "echo \"{}\" | festival --tts".format(Str)
                    subprocess.call(ex, shell=True)
            
            queueLock.release()
            
            annunciateCondition.acquire()
            annunciateCondition.wait()
                          
def enqueueMessage(message):
    threading._start_new_thread(messageProducer, (message,))

def messageProducer(message):
    queueLock.acquire()
    messageQueue.put(message)
    annunciateCondition.acquire()
    annunciateCondition.notify_all()
    annunciateCondition.release()
    queueLock.release()

annunciator = annunciatorThread()
annunciator.start()
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
    
    enqueueMessage(msg)
    