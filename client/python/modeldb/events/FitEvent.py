"""
Event indicating estimator was used to fit model.
"""
from modeldb.events.Event import Event
from ..thrift.modeldb import ttypes as modeldb_types
import json


class FitEvent(Event):
    """
    Class for creating and storing FitEvents
    """

    def __init__(self, model, spec, df, metadata={}):
        self.model = model
        self.spec = spec
        self.df = df
        self.metadata = metadata

    def make_event(self, syncer):
        """
        Constructs a thrift FitEvent object with appropriate fields.
        """
        syncable_transformer = syncer.convert_model_to_thrift(self.model)
        model_spec = syncer.convert_spec_to_thrift(self.spec)
        syncable_dataframe = syncer.convert_df_to_thrift(self.df)
        columns = syncer.set_columns(self.df)
        fe = modeldb_types.FitEvent(syncable_dataframe, model_spec,
                                    syncable_transformer, columns,
                                    [], [], syncer.experiment_run.id)
        fe.metadata = json.dumps(self.metadata, default=str)
        return fe

    def associate(self, res, syncer):
        """
        Stores the generated ids into dictionary.
        """
        syncer.store_object(self.df, res.dfId)
        syncer.store_object(self.spec, res.specId)
        syncer.store_object(self.model, res.modelId)
        syncer.store_object(self, res.eventId)

    def sync(self, syncer):
        """
        Stores FitEvent on the server.
        """
        fe = self.make_event(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeFitEvent(fe)
        self.associate(res, syncer)
