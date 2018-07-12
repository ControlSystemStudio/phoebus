# ScanErrorHandler that ignores any error

from org.csstudio.scan.command import ScanErrorHandler

class OnErrorContinue(ScanErrorHandler):
    
    def handleError(self, command, error, context):
        print "Ignoring error from " + str(command)
        return ScanErrorHandler.Result.Continue

