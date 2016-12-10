import sys
import os

output_dir = sys.argv[1]

def filename(num_duplications):
    return os.path.join(output_dir, "output_%s.csv" % (str(num_duplications),))

def time_for_method(fname):
    with open(fname) as f:
        lines = f.readlines()[1:]
        lines = [line.replace("\n", "").split(", ") for line in lines]
        lines = [(l[0].strip(), float(l[1].strip())) for l in lines]
        return lines

times_for_method = {}
for num_duplications in (1, 5, 10, 20, 40, 60, 80, 100, 140, 160, 180, 250, 300, 350, 400):
    fname = filename(num_duplications)
    if os.path.isfile(fname):
        for (method, time) in time_for_method(fname):
            if method not in times_for_method:
                times_for_method[method] = []
            times_for_method[method].append((num_duplications, time))

for (method, times) in times_for_method.iteritems():
    for (num_duplications, time) in times:
        print(method + ", " + str(num_duplications) + ", " + str(time))
