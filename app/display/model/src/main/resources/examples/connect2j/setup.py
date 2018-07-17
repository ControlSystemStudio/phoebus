#!/usr/bin/env python

from distutils.core import setup

setup(name='connect2j',
      py_modules=['connect2j'],
      version='1.0',
      description='Connect to Java. Essentially wraps selected py4j methods.',
      long_description='Connect to Java using py4j. Designed to work with Java PythonGatewaySupport \
                        class (in package org.csstudio.display.builder.runtime.script.internal)',
      author='Amanda Carpenter',
     )