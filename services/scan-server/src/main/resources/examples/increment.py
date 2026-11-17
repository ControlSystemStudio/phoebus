# increment a PV (device)
# Takes two arguments:
# 1) Name of PV from which to read-inc-write
# 2) increment amount
from org.csstudio.scan.command import ScanScript

class Increment(ScanScript):
    def __init__(self, data_pv, inc_val):
        self.data_pv = data_pv
        self.inc_val = inc_val

    def getDeviceNames(self):
        return [ self.data_pv ]

    def run(self, context):
        print("Running Increment")
        data = context.read(self.data_pv)
        print("type(data)="+str(type(data)))
        data += float(self.inc_val)
        context.write(self.data_pv,data)