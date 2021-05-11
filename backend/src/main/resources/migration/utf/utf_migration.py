import csv
import sys
if len(sys.argv) != 3:
    print("Missing argument: $ python utf_migration.py <tables.csv> <database_name>\n"
          "Table row format: database_name	TABLE_NAME	COLUMN_NAME	DATA_TYPE	maximum_length", file=sys.stderr)
    exit(1)
with open(sys.argv[1], newline='') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        if row['database_name'] == sys.argv[2]:
            if row['DATA_TYPE'] == 'varchar':
                type = "varchar({})".format(row['maximum_length'])
            else:
                type = row['DATA_TYPE']
            print("ALTER TABLE {} MODIFY COLUMN {} {} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
                  .format(row['TABLE_NAME'], row['COLUMN_NAME'], type))
