"""
This script reads a CSV and duplicates its rows until it has the desired row
count (this will be ignored if it is less than the number of rows in the dataset).

Execute this script as
python grow_dataset.py <path_to_dataset> <desired_number_of_rows>

The script assumes that the first line in the dataset is the header.

The output CSV will be printed to STDOUT.
"""
import sys

path_to_dataset = sys.argv[1]
desired_num_rows = int(sys.argv[2])

lines = []
header = None
with open(path_to_dataset) as f:
    for line in f:
        line = line.replace("\n", "").replace("\r", "")
        if header is None:
            header = line
            continue
        lines.append(line)

num_lines = len(lines)
desired_num_rows = max(desired_num_rows, num_lines)
num_printed = 0
index = 0

print(header)
while num_printed < desired_num_rows:
    print(lines[index])
    index = (index + 1) % num_lines
    num_printed += 1
