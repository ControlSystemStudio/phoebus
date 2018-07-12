from org.csstudio.scan.command import ScanScript

class MyScanScript(ScanScript):
    def __init__(self, arg1):
        print("MyScanScript initialized with " + str(arg1))
        self.name = arg1

    def getDeviceNames(self):
        return [ "loc://result" ]

    def run(self, context):
        print("MyScanScript called")
        context.write("loc://result", "Hello " + self.name)
