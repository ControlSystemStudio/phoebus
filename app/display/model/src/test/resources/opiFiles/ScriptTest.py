import os
from org.csstudio.opibuilder.scriptUtil import ConsoleUtil
from org.csstudio.opibuilder.scriptUtil import FileUtil
from org.csstudio.opibuilder.scriptUtil import PVUtil
from org.csstudio.opibuilder.util import ResourceUtil

# read pv value holding error code
pv0 = PVUtil.getLong(pvs[0])
    