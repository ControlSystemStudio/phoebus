# Example ScriptCommand script
#
# Takes two arguments:
# 1) Name of logged PV from which to get logged data
# 2) Name of PV to which average is written
from org.csstudio.scan.command import ScanScript

import numjy as np

class ComputeAverage(ScanScript):
    def __init__(self, data_pv, avg_pv):
        self.data_pv = data_pv
        self.avg_pv = avg_pv
        # print("Using numjy " + str(np))

    def getDeviceNames(self):
        return [ self.avg_pv ]

    def run(self, context):
        print("ComputeAverage of %s into %s" % (self.data_pv, self.avg_pv))
        # Fetching data for N channels returns N x samples
        data = np.array(context.getData(self.data_pv))
        # Get values for the first (only) channel
        values = data[0]
        print("Data   : " + str(values))
        if len(values) > 0:
           avg = np.sum(values) / len(values)
        else:
           avg = np.nan
        print("Average: " + str(avg))
        context.write(self.avg_pv, avg)
