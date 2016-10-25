#!/usr/bin/env python

# update_thrift_file.py language src_path_for_thrift_file dest_path_folder 

import os
import sys

lang = sys.argv[1]
src_path = sys.argv[2]
dest_path = sys.argv[3]

try:
    os.mkdir(dest_path)
except:
    pass
data = open(src_path, "r").read()
f = open(dest_path + "/ModelDB.thrift", "wb")
if lang == "scala":
    f.write("namespace scala modeldb\n")
f.write(data)
f.close()