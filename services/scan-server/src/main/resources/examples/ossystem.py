# os.system
import os

from org.csstudio.scan.command import ScanScript

class OsSystem(ScanScript):
    def __init__(self, command):
        self.command = command

    def getDeviceNames(self):
        return []

    def run(self, context):
        print("Running OsSystem")
        os.system(self.command)
