import sys
import numpy as np
import os

import matplotlib.pyplot as plt

output_dir = sys.argv[1]

def filename(dataset, workflow, size, ext):
    return os.path.join(output_dir, "%s_%s_%s.%s" % (dataset, workflow, size, ext))

def get_overhead_percent(filename):
    with open(filename) as f:
        time_for_op = {}
        op_time = 0.0
        for line in f:
            line = line.replace("\n", "")
            if "Operation" in line:
                continue
            if "Full timer" in line:
                total_time = float(line.split(", ")[1])
                for op in time_for_op:
                    arr = time_for_op[op]
                    time_for_op[op] = [t / total_time for t in arr]
                return (op_time / total_time, time_for_op)
            if "Syncing" in line:
                spl_line = line.split(", ")
                indiv_op_time = float(spl_line[1])
                op = spl_line[0].replace("Syncing ", "")
                if op not in time_for_op:
                    time_for_op[op] = []
                time_for_op[op].append(indiv_op_time)
            op_time += float(line.split(", ")[1])

def get_table_sizes(filename):
    with open(filename) as f:
        lines = []
        for line in f:
            line = line.replace("\n", "").split(", ")
            table = line[0]
            count = int(line[1])
            lines.append((table, count))
        return lines

modelsizes = {}
dbsizes = {}
overhead = {}
tablesizes = {}
overhead_for_op = {}
for dataset in ("imdb", "housing", "animal"):
    for workflow in ("simple", "full", "exploratory"):
        dbsizes[(dataset, workflow)] = []
        modelsizes[(dataset, workflow)] = []
        overhead[(dataset, workflow)] = []
        for size in (1, 10000, 20000, 40000, 70000, 100000, 200000, 400000, 700000, 1000000):
            fname = filename(dataset, workflow, size, "dbsize")
            if os.path.isfile(fname):
                with open(fname) as f:
                    dbsize = f.read().split("\t")[0]
                    orig_dbsize = dbsize
                    if "M" in dbsize:
                        dbsize = float(dbsize.replace("M", ""))
                    elif "K" in dbsize:
                        dbsize = float(dbsize.replace("K", "")) / 1024.0
                    dbsizes[(dataset, workflow)].append((orig_dbsize, dbsize))
            fname = filename(dataset, workflow, size, "modelsize")
            if os.path.isfile(fname):
                with open(fname) as f:
                    modelsize = f.readlines()[-1].split("\t")[0]
                    orig_modelsize = modelsize
                    if "M" in modelsize:
                        modelsize = float(modelsize.replace("M", ""))
                    elif "K" in modelsize:
                        modelsize = float(modelsize.replace("K", "")) / 1024.0
                    modelsizes[(dataset, workflow)].append((orig_modelsize, modelsize))
            fname = filename(dataset, workflow, size, "csv")
            if os.path.isfile(fname):
                (ohead, time_for_op) = get_overhead_percent(fname)
                for op in time_for_op:
                    if op not in overhead_for_op:
                        overhead_for_op[op] = []
                    overhead_for_op[op].extend(time_for_op[op])
                overhead[(dataset, workflow)].append((size, ohead))
            fname = filename(dataset, workflow, size, "tablesizes")
            if os.path.isfile(fname):
                if size == 1:
                    tablesizes[(dataset, workflow)] = get_table_sizes(fname)

print "Database Sizes"
for (k, v) in dbsizes.iteritems():
    dbsize = max([x[1] for x in v])
    dbsize = [x[0] for x in v if x[1] == dbsize][0]
    print ", ".join((k[0], k[1], dbsize))

print "Model Sizes"
for (k, v) in modelsizes.iteritems():
    dbsize = max([x[1] for x in v])
    dbsize = [x[0] for x in v if x[1] == dbsize][0]
    print ", ".join((k[0], k[1], dbsize))

def cleanOp(op):
    if "GridSearchCrossValidationEvent" in op:
        return "GridSearchCVEvent"
    elif "GridCell" in op:
        return "GridCellCV"
    elif "TreeModelComponent" in op:
        return "TreeModelComp"
    elif "ModelObjectiveHistory" in op:
        return "ModelObjHistory"
    else:
        return op
print "Time overhead per operation"
for op in overhead_for_op:
    overhead_for_op[op] = sum(overhead_for_op[op]) * 1.0 / len(overhead_for_op[op])
plt.figure(figsize=(20, 10))
pairs = [(cleanOp(op), overhead_for_op[op]) for op in overhead_for_op]
objects = [x[0] for x in pairs]
performance = [x[1] * 100 for x in pairs]
y_pos = np.arange(0, len(objects) * 2, 2)
plt.barh(y_pos, performance, align='center', alpha=0.5)
plt.yticks(y_pos, objects, fontsize=15)
plt.autoscale(tight=True)
plt.xticks(fontsize=20)
plt.xlabel('Time Overhead (% of overall runtime)', fontsize=20)
plt.title('Average Time Overhead by Event', fontsize=30)
plt.show()

for (k, v) in overhead.iteritems():
    plt.scatter([x[0] for x in v], [y[1]*100 for y in v])
    plt.title("Time Overhead (as percentage of overall time) for %s %s" % k)
    plt.xlabel("Dataset size (num rows)")
    plt.ylabel("ModelDB Overhead time (%)")
    plt.xlim([0, 1000000])
    plt.ylim([0, 70])
    plt.show()

for (k, v) in tablesizes.iteritems():
    plt.figure(figsize=(18, 10))
    objects = [cleanOp(x[0]) for x in v]
    scaler = 1
    y_pos = np.arange(0, len(objects) * scaler, scaler)
    performance = [x[1] for x in v]
    plt.barh(y_pos, performance, align='center', alpha=0.5)
    plt.autoscale(tight=True)
    plt.yticks(y_pos, objects, fontsize=15)
    plt.xticks(fontsize=20)
    plt.xlabel('Number of Rows', fontsize=20)
    plt.title('Table Sizes for %s %s' % k, fontsize=30)
    plt.show()
