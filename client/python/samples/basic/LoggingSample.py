from modeldb.basic.ModelDbSyncerBase import *
from datetime import datetime

# Create a syncer using a convenience API
syncer_obj = Syncer.create_syncer("Sample Project", "test_user", \
    "using modeldb logging functions")

client = syncer_obj.client


# get relevant project ids with case insensitive keys and case sensitive values
projectIds = client.getProjectIds({'author':'test_user'})
# update projects
for projectId in projectIds:
    client.updateProject(projectId, 'name', "Sample Logging Project")

# get all model ids
allModelIds = client.getModelIds({})
# get relevant model ids with case sensitive key-value pairs
modelIds = client.getModelIds({'TAG':'train', 'TYPE':'Normal distributions'})
# create and update fields of models
for modelId in modelIds:
    # create or update scalar fields with string values
    client.createOrUpdateScalarField(modelId, 'PATH', 'new/path/to/model', 'string')
    # or with dates (use .isoformat() for datetimes)
    client.createOrUpdateScalarField(modelId, 'date-created', datetime.now().isoformat(), 'datetime')
    # or with booleans
    client.createOrUpdateScalarField(modelId, 'is-production', 'false', 'bool')
    # or with doubles
    # update fields nested within vectors using mongodb's dot notation
    client.createOrUpdateScalarField(modelId, 'METRICS.0.VALUE', '0.25', 'double')

    # create vector fields in nested locations using mongodb's dot notation
    # e.g. model[CONFIG][values] = []
    vectorConfig = {} # specify configurations for the vector (this is non-functional for now)
    client.createVectorField(modelId, 'CONFIG.values', vectorConfig)
    # append to vector fields
    values = [150, 20, 300]
    for i in xrange(len(values)):
        # use int value types
        client.appendToVectorField(modelId, 'CONFIG.values', str(values[i]), 'int')
    # update vector fields at a specific index
    client.updateVectorField(modelId, 'CONFIG.values', 0, 'new value', 'string')


# close thrift client
syncer_obj.closeThriftClient()
