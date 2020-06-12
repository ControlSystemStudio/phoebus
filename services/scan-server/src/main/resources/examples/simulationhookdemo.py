from org.csstudio.scan.server import SimulationHook
from org.phoebus.core.vtypes import VTypeHelper

class SimulationHookDemo(SimulationHook):

    def getRate(self, context):
        """Get rep. rate from simulated device, with fallback"""
        try:
            return VTypeHelper.toDouble(context.getDevice("rate").read())
        except:
            return 60.0

    def handle(self, command, context):
        """command - ScanCommand to simulate
           context - SimulationContext
           return True if handling the command, False to get default
        """
        print("Simulating " + str(command))
        cmd = command.getCommandID()

        if cmd == "comment":
            # Suppress comments that start with "#"
            comment = command.getProperty("comment")
            return comment.startswith("#")
        
        if cmd == "log":
            # Ignore log commands in simulation
            return True
        
        if cmd == "set":
            name = command.getProperty("device_name")
            if name == "rate":
                # Changing rate always takes a fixed amount of time
                value = command.getProperty("value")
                context.logExecutionStep("Set beam rate to %g Hz" % value, 5.0);
                # Do update the simulated device!
                context.getDevice("rate").write(value)
                return True
        
        if cmd == "wait":
            name = command.getProperty("device_name")
            if name == "charge":
                charge = command.getProperty("desired_value")

                # Time spent waiting for beam charge depends on rate
                rate = self.getRate(context)
                time = 60.0*60.0 * charge * 60/rate
                                
                context.logExecutionStep("Wait for %.2f Coulomb at %.0f Hz" % (charge, rate), time);
                return True

        # For commands not specifically handled, use default simulation
        return False
