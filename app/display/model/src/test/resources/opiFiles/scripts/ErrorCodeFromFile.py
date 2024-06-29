# This script looks for an error code in a file
# to display the corresponding error message

# FILE is expected to be the name of a file in the same directory as the OPI
# But it can also be a relative path for a file in the workspace
# eg. /CSS/MSG_VAR.txt, is a valid path if MSG_VAR.txt and CSS project exist in the workspace
# Finally it can also be an absolute path.

# The provided file is supposed to have line looking like :
# <error_code>;<error_message>

# pv0 is the PV holding the current error code

# NOTE: Python's built-in open function is used instead of FileUtil.readTextFile
# because it provides a better support of the file encoding

# FILE = "MSG_VAR.txt"  # eg. valid filename

import os
from org.csstudio.opibuilder.scriptUtil import ConsoleUtil
from org.csstudio.opibuilder.scriptUtil import FileUtil
from org.csstudio.opibuilder.scriptUtil import PVUtil
from org.csstudio.opibuilder.util import ResourceUtil

# read pv value holding error code
pv0 = PVUtil.getLong(pvs[0])
FILE = widget.setPropertyValue("tooltip")

# default display in case file is not found
widget.setPropertyValue("text", str(FILE)+": file not found")

# compute path relative to workspace based on widget's opi localisation
fileIPath = ResourceUtil.getPathFromString(FILE)
filePathInWorkspace = str(
    ResourceUtil.buildAbsolutePath(widget.getWidgetModel(), fileIPath)
    )

filepath = FILE # value in case we provided an absolute path
# prepend with workspace path if existing project in workspace
if FileUtil.workspacePathToSysPath(filePathInWorkspace):
    filepath = FileUtil.workspacePathToSysPath(filePathInWorkspace)
else:
    ConsoleUtil.writeInfo("Not a workspace path: " + filepath)

# search error code to retrieve corresponding error message
with open(filepath) as input_file:

    # default display in case error code is not found in file
    widget.setPropertyValue("text", str(pv0)+": unkown error code")

    # look for error message corresponding to error code
    for a_line in input_file:
        if ";" in a_line:  # only try spliting if semi-colon present
            # only work on first semi-colon
            err_code, err_message = a_line.strip().split(";", 1)
            if int(err_code) == pv0:
                widget.setPropertyValue("text", err_message)
                break
