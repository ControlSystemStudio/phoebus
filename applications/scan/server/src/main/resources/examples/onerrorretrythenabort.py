# ScanErrorHandler that retries several times, then aborts

from org.csstudio.scan.command import ScanErrorHandler

class OnErrorRetryThenAbort(ScanErrorHandler):
    def __init__(self):
        # Initialize the number of retries 
        self.retries = 3
    
    def handleError(self, command, error, context):
        self.retries -= 1
        if self.retries > 0:
            print "Retrying " + str(command) + " " + str(self.retries) + " more times ..."
            return ScanErrorHandler.Result.Retry
        else:    
            return ScanErrorHandler.Result.Abort

