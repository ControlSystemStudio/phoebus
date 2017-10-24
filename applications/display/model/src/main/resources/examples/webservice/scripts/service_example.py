# pvs[0]: initial trigger
# pvs[1]: Re-fetch?
# import sys
# print("Search path:\n" + "\n".join(sys.path))

from org.csstudio.display.builder.runtime.script import PVUtil

if PVUtil.getInt(pvs[1]):
    message = "Re-fetching logbook entries"
else:
    message = "Fetching Logbook entries..."

widget.setValue([ [ "-", message ] ])

from my_service import read_html, create_table
html = read_html()
widget.setValue(create_table(html))
