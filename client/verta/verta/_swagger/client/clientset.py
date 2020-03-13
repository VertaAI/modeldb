from .._public.artifactstore.api.ArtifactStoreApi import ArtifactStoreApi
from .._public.modeldb.api.CommentApi import CommentApi
from .._public.modeldb.api.DatasetServiceApi import DatasetServiceApi
from .._public.modeldb.api.DatasetVersionServiceApi import DatasetVersionServiceApi
from .._public.modeldb.api.ExperimentRunServiceApi import ExperimentRunServiceApi
from .._public.modeldb.api.ExperimentServiceApi import ExperimentServiceApi
from .._public.modeldb.api.HydratedServiceApi import HydratedServiceApi
from .._public.modeldb.api.JobApi import JobApi
from .._public.modeldb.api.LineageApi import LineageApi
from .._public.modeldb.api.ProjectServiceApi import ProjectServiceApi

class ClientSet(object):
    def __init__(self, client):
        self.client = client

        self.artifactStoreService = ArtifactStoreApi(client, "/api/v1/modeldb")
        self.commentService = CommentApi(client, "/api/v1/modeldb")
        self.datasetService = DatasetServiceApi(client, "/api/v1/modeldb")
        self.datasetVersionService = DatasetVersionServiceApi(client, "/api/v1/modeldb")
        self.experimentRunService = ExperimentRunServiceApi(client, "/api/v1/modeldb")
        self.experimentService = ExperimentServiceApi(client, "/api/v1/modeldb")
        self.hydratedService = HydratedServiceApi(client, "/api/v1/modeldb")
        self.jobService = JobApi(client, "/api/v1/modeldb")
        self.lineageService = LineageApi(client, "/api/v1/modeldb")
        self.projectService = ProjectServiceApi(client, "/api/v1/modeldb")
