# Example script that updates the 'file' of
# an embedded display widget.
#
# Meant as an embedded display widget test.
# Unlikely to be useful in a production setup
# because displays should only change their fundamental
# content in response to operator input.
from org.csstudio.display.builder.runtime.script import PVUtil

# PV is supposed to cycle through values 0, 1, 2, 3
sel = PVUtil.getDouble(pvs[0])
if sel < 0.5:
    # Initial value: Show some file with macros
    widget.getPropertyValue("macros").add("M", "Value 1")
    widget.setPropertyValue("file", "a.bob")
elif sel < 1.5:
    # Next value: Show same file with different macros
    widget.getPropertyValue("macros").add("M", "Value 2")
    # Need to change the file name and then revert back to force a reload
    widget.setPropertyValue("file", "")
    widget.setPropertyValue("file", "a.bob")
elif sel < 2.5:
    # Different file
    widget.setPropertyValue("file", "b.bob")
else:
    # File that doesn't actually exist.
    # Widget will indicate error.
    widget.setPropertyValue("file", "missing.bob")
