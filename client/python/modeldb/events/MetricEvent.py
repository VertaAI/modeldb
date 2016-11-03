"""
Event indicating that a metric has been computed for a model's prediction.
"""
from modeldb.events.Event import *

class MetricEvent(Event):
    """
    Class for creating and storing MetricEvents
    """
    def __init__(self, df, model, labelCol, predictionCol, metricType, metricValue):
        self.df = df
        self.model = model
        self.metric_type = metricType
        self.metric_value = metricValue
        self.label_col = labelCol
        self.prediction_col = predictionCol

    def makeEvent(self, syncer):
        """
        Constructs a thrift MetricEvent object with appropriate fields.
        """
        syncable_transformer = syncer.convertModeltoThrift(self.model)
        syncable_dataframe = syncer.convertDftoThrift(self.df)
        me = modeldb_types.MetricEvent(
            syncable_dataframe,
            syncable_transformer,
            self.metric_type,
            self.metric_value,
            self.label_col,
            self.prediction_col,
            syncer.experimentRun.id)
        return me

    def associate(self, res, syncer):
        """
        Stores the server response ids into dictionary.
        """
        df_id = id(self.df)
        syncer.storeObject(df_id, res.dfId)
        syncer.storeObject(self.model, res.modelId)
        syncer.storeObject(self, res.eventId)

    def sync(self, syncer):
        """
        Stores MetricEvent on the server.
        """
        me = self.makeEvent(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeMetricEvent(me)
        self.associate(res, syncer)
