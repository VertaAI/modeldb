# note this is hacky; this script creates a k8s secret using config.yaml
import os

SECRET_NAME = "modeldb-backend-config-secret"
INPUT_CONFIG = "config/config.yaml"
OUTPUT_CONFIG = "../chart/modeldb/templates/modeldb-backend-config-secret.yaml"
SPACES_TO_TABS=2
SPACE=' '

with open(INPUT_CONFIG, 'r') as f:
    inner_config = (f.readlines())

with open(OUTPUT_CONFIG, 'w') as f:
    s = (
            "apiVersion: v1\n"
            "kind: Secret\n", 
            "metadata:\n",
            "  name: {}\n".format(SECRET_NAME),
            "type: Opaque\n",
            "stringData:\n",
            "  config.yaml: |-\n"
        ) 
    f.write(''.join(s))
    for line in inner_config:
        if line != "\n":
            f.write(''.join([SPACES_TO_TABS * SPACE * 2]) + line)
        else:
            f.write(line)

