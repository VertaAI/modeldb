#!/usr/bin/env python

import os

try:
    os.mkdir('./thrift/')
except:
    pass
data = open("../../../../thrift/ModelDB.thrift", "r").read()
f = open("./thrift/ModelDB.thrift", "wb")
f.write(data)
f.close()
