"""
Stores Pipeline Events on server.
"""
from modeldb.events.Event import Event
from ..thrift.modeldb import ttypes as modeldb_types

# This class creates and stores a pipeline event in the database.


class PipelineEvent(Event):
    """
    Class for creating and storing PipelineEvents
    """

    def __init__(self, firstPipelineEvent, transformStages, fitStages):
        self.first_pipeline_event = firstPipelineEvent
        self.transform_stages = transformStages
        self.fit_stages = fitStages

    def make_event(self, syncer):
        """
        Constructs a thrift PipelineEvent object using the
        captured transform and fit stages.
        """
        pipeline_first_fit_event = self.first_pipeline_event.make_event(syncer)
        transform_event_stages = []
        fit_event_stages = []
        for index, transform_event in self.transform_stages:
            transform_event_stages.append(
                modeldb_types.PipelineTransformStage(
                    index, transform_event.make_event(syncer)))
        for index, fit_event in self.fit_stages:
            fit_event_stages.append(
                modeldb_types.PipelineFitStage(
                    index, fit_event.make_event(syncer)))
        pe = modeldb_types.PipelineEvent(
            pipeline_first_fit_event, transform_event_stages, fit_event_stages,
            syncer.experiment_run.id)
        return pe

    def associate(self, res, syncer):
        """
        Stores each of the individual fit and transform event ids into
        dictionary.
        """
        self.first_pipeline_event.associate(res.pipelineFitResponse, syncer)
        for (transform_res, (index, te)) in zip(res.transformStagesResponses,
                                                self.transform_stages):
            te.associate(transform_res, syncer)
        for (fit_res, (index, fe)) in zip(
                res.fitStagesResponses, self.fit_stages):
            fe.associate(fit_res, syncer)

    def sync(self, syncer):
        """
        Stores PipelineEvent on the server.
        """
        pe = self.make_event(syncer)
        thrift_client = syncer.client
        res = thrift_client.storePipelineEvent(pe)
        self.associate(res, syncer)
