#!/usr/bin/env python

import os

try:
    os.mkdir('./src/main/thrift/')
except:
    pass
data = open("../thrift/ModelDB.thrift", "r").read()
f = open("./src/main/thrift/ModelDB.thrift", "wb")
f.write(data)
f.close()
