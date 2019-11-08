# Close all other tabs within the same pane
#
# Can be attached to a display, triggered by "loc://init(0)"
# to run when display is opened.
#
# Example for obtaining the DisplayRuntimeInstance and DockItem
# of a display to then interact with them

from java.lang import Runnable
from javafx.application import Platform
from org.csstudio.display.builder.runtime.app import DisplayRuntimeInstance

display = widget.getTopDisplayModel()

# Interactions with UI must run on the UI thread
class CloseOther(Runnable):
    def run(self):
        # Get DockItem for this display
        this_item = DisplayRuntimeInstance.ofDisplayModel(display).getDockItem()
        # Close all _other_ items (tabs) within this pane
        for item in this_item.getDockPane().getDockItems():
            if item != this_item:
                print "CLOSE " + str(item)
                item.close()

Platform.runLater(CloseOther())
