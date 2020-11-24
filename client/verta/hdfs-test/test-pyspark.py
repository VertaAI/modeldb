import hashlib

def hash_content(content):
    import hashlib
    return hashlib.md5(content).hexdigest()

from pyspark import SparkContext
sc = SparkContext("local")

from verta.dataset import HDFSPath
blob = HDFSPath.with_spark(sc, 'hdfs://ip-10-0-147-175.ec2.internal:8020/data/census/*')
print(blob)

# files = sc.binaryFiles('hdfs://ip-10-0-147-175.ec2.internal:8020/data/census/*')
# names = files.map(lambda x: (x[0], hashlib.md5(x[1]).hexdigest()))
# print(names.collect())
