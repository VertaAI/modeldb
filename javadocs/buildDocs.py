import glob

#vals = glob.glob("..\server\src\main\java\edu\mit\csail\db\ml\*\*.java")
vals = glob.glob("..\client\scala\libs\spark.ml\src\main\scala-2.11\edu\mit\csail\db\ml\modeldb\*\*.scala")
files = reduce(lambda s,t: s+ " " + t,vals)

print "javadoc -private " + files