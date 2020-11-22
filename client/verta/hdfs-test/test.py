from hdfs.client import InsecureClient

client = InsecureClient("http://ip-10-0-147-175.ec2.internal:50070", user="ec2-user")

print(client.list('/data/census'))
with client.read('/data/census/census-test.csv') as reader:
    print(reader.read())
