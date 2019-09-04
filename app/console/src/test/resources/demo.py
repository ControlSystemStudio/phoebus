import time, sys, threading, code


class DemoThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
    
    def run(self):
        i = 1
        while True:
            print("Demo %d" % i)
            sys.stdout.flush()
            i += 1
            time.sleep(1)
            if i>5:
                break
                
demo = DemoThread()
demo.start()

print("Running thread...")

code.interact(local=locals())