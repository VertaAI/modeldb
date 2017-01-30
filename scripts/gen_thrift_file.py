#!/usr/bin/env python

# update_thrift_file.py language src_path_for_thrift_file dest_path_folder 

'''
The client has to be built using Apache thrift. In order to build the client, thrift needs
a thrift file. If it is being built for java or python, thrift needs the file as-is.
If it is being built in scala, it needs an additional line

This file uses the template thrift file (that works for the entire project) and adds
a line if it is needed for scala.

Usage:
update_thrift_file.py language src_path_for_thrift_file destination_folder_of_modified_file

language:
One of [python, scala, java] to generate the thrift file for

src_path_for_thrift_file:
Where the original thrift file (modeldb/thrift/ModelDB.thrift) is located.

destination_folder_of_modified_file:
Where to store the new thrift file, which may or may not be modified.
'''

#os to make directories
import os

#sys to read command line arguments
import sys

# The language to generate for
lang = sys.argv[1]

# The original thrift file
src_path = sys.argv[2]

# The folder to output the potentially modified thrift file too
dest_path = sys.argv[3]

# Make the directory if it needs to be made
try:
    os.makedirs(dest_path)
except:
    pass

# read the source file
data = open(src_path, "r").read()

# Open the output file (where to copy to)
f = open(dest_path + "/ModelDB.thrift", "w")

# If it's in scala, write the namespace first, else do nothing
if lang == "scala":
    f.write("namespace scala modeldb\n")

# Write the thrift file out into the new destination
f.write(data)

# Close and save the file
f.close()
