const getConfig = require('../getConfig');

const config = getConfig();
const userWorkspace = config.user.workspace;

const makeIndexRoute = () => `${config.baseURL}/`;

const makeProjectsRoute = ({ workspaceName = userWorkspace} = {}) => `${config.baseURL}/${workspaceName}/projects`;
const makeProjectRoute = ({ projectId, workspaceName }) => `${makeProjectsRoute({ workspaceName })}/${projectId}`;
const makeProjectSummary = (params) => `${makeProjectRoute(params)}/summary`;
const makeExperimentsRoute = (params) => `${makeProjectRoute(params)}/experiments`;
const makeExperimentRunsRoute = (params) => `${makeProjectRoute(params)}/exp-runs`;
const makeExperimentRunRoute = ({projectId, experimentRunId, workspaceName}) => `${makeExperimentRunsRoute({ workspaceName, projectId })}/${experimentRunId}`;
// /projects/:projectId/exp-runs/compare/:modelRecordId1/:modelRecordId2'
const makeCompareExperimentRunsRoute = ({ projectId, workspaceName, experimentRunId1, experimentRunId2 }) =>
    `${makeExperimentRunsRoute({ projectId, workspaceName })}/compare/${experimentRunId1}/${experimentRunId2}`;
const projectsRoutes = {
    makeProjectsRoute,
    makeProjectRoute,
    makeProjectSummary,
    makeExperimentsRoute,
    makeExperimentRunsRoute,
    makeExperimentRunRoute,
    makeCompareExperimentRunsRoute,
};

const makeDatasetsRoute = ({ workspaceName = userWorkspace} = {}) => `${config.baseURL}/${workspaceName}/datasets`;
const makeDatasetRoute = ({ datasetId, workspaceName }) => `${makeDatasetsRoute({ workspaceName })}/${datasetId}`;
const makeDatasetSummary = (params) => `${makeDatasetRoute(params)}/summary`;
const makeDatasetVersionsRoute = (params) => `${makeDatasetRoute(params)}/versions`;
const makeDatasetVersionRoute =
    ({datasetId, datasetVersionId, workspaceName}) => `${makeDatasetVersionsRoute({ workspaceName, datasetId })}/${datasetVersionId}`;
const datasetsRoutes = {
    makeDatasetsRoute,
    makeDatasetRoute,
    makeDatasetSummary,
    makeDatasetVersionsRoute,
    makeDatasetVersionRoute
};

module.exports = {
    projectsRoutes,
    datasetsRoutes,
    makeIndexRoute
};
