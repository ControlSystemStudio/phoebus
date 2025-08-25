#!/bin/env python3

import argparse
from os import path
from glob import glob
import re
import xml.etree.ElementTree as ET


parser = argparse.ArgumentParser(
		     description='Check if .classpath entries exist. If not, suggest alternate version')
args = parser.parse_args()
     

xml = ET.parse(".classpath")
root = xml.getroot()

for entry in root.iter('classpathentry'):
    if entry.get('kind') == 'lib':
        jar = entry.get('path')
        if path.exists(jar):
            pass
        else:
            pattern = re.sub(r'[0-9.]+', r'*', jar)
            # print(pattern)
            alt = glob(pattern)
            # print(alt)
            if len(alt) == 1:
                update = alt[0]
                print("%-60s  ----->  use %s" % (jar, update))
            elif len(alt) > 1:
                update = " or ".join(alt)
                print("%-60s  ----->  use %s" % (jar, update))
            else:
                print("%-60s is missing, no replacement found" % jar)


