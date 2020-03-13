const { default: axios } = require('axios');

const { setupAxiosInstance } = require('./axiosHelpers');
const getConfig = require('../getConfig');
const config = getConfig();

setupAxiosInstance(axios, { email: config.user.email, developerKey: config.user.developerKey });

const getUserByEmail = (email) => {
    return axios.get('/v1/uac-proxy/uac/getUser', { params: { email } }).then(({ data }) => data);
};
const updateUsername = ({ username, email, developerKey }) => {
    const _axios = setupAxiosInstance(axios.create(), { email, developerKey });

    return getUserByEmail(email)
        .then(({ verta_info: userVertaInfo }) => {
            if (userVertaInfo.username === username) {
                return Promise.resolve({});
            } else {
                return _axios.post('/v1/uac-proxy/uac/updateUser', {
                    info: {
                      verta_info: {
                        user_id: userVertaInfo.user_id,
                        username,
                      },
                    },
                  });
            }
        });
};

const createProjects = (projects) => {
    return Promise.all(
        projects.map((project) => {
            return axios.post('/v1/modeldb/project/createProject', project);
        }),
    );
};
const getProjects = async ({ workspace_name } = {}) => {
    const response = await axios.get('/v1/modeldb/project/getProjects', { params: { workspace_name } });
    return (response.data.projects || []);
};
const deleteAllProjects = () => {
    return getProjects()
        .then((projects) => projects.length > 0 ? axios.delete('/v1/modeldb/project/deleteProjects', {
            data: { ids: projects.map(({ id }) => id) },
        }) : Promise.resolve())
};
const deleteProjectsByIds = async (ids) => {
    if (ids.length === 0) {
        return;
    }
    return await axios.delete('/v1/modeldb/project/deleteProjects', {
        data: { ids },
    });
};
const createExperiments = async (experiments) => {
    return Promise.all(experiments.map((experiment) => axios.post('/v1/modeldb/experiment/createExperiment', experiment)));
};
const createExperimentRuns = async (project, experiment, experimentRuns) => {
    const [{ data: { project: createdProject } }] = await createProjects([project]);
    const [{ data: { experiment: createdExperiment } }] = await createExperiments([({ project_id: createdProject.id, ...experiment })]);
    const createdExperimentRuns = await Promise.all(
        experimentRuns.map((experimentRun) => axios.post('/v1/modeldb/experiment-run/createExperimentRun', ({
            ...experimentRun,
            project_id: createdProject.id,
            experiment_id: createdExperiment.id,
        })))
    );
    return {
        projectId: createdProject.id,
        experimentRuns: createdExperimentRuns.map(({ data: { experiment_run } }) => experiment_run),
    };
};
const createExperimentRun = async (project, experiment, experimentRun) => {
    const experimentRuns = [experimentRun];
    const [{ data: { project: createdProject } }] = await createProjects([project]);
    const [{ data: { experiment: createdExperiment } }] = await createExperiments([({ project_id: createdProject.id, ...experiment })]);
    return await Promise.all(
        experimentRuns.map((experimentRun) => axios.post('/v1/modeldb/experiment-run/createExperimentRun', ({
            ...experimentRun,
            project_id: createdProject.id,
            experiment_id: createdExperiment.id,
        })))
    )
    .then(([{ data: { experiment_run } }]) => ({ projectId: experiment_run.project_id, experimentRunId: experiment_run.id }));
};

const createDatasets = (datasets) => {
    return Promise.all(
        datasets.map((dataset) => {
            return axios.post('/v1/modeldb/dataset/createDataset', dataset);
        }),
    );
};
const deleteDatasetsByIds = async (ids) => {
    if (ids.length === 0) {
        return;
    }
    return await axios.delete('/v1/modeldb/dataset/deleteDatasets', {
        data: { ids },
    });
};
const getDatasets = async ({ workspace_name } = {}) => {
    const response = await axios.get('/v1/modeldb/dataset/getAllDatasets', { params: { workspace_name } });
    return (response.data.datasets || []);
};
const deleteAllDatasets = () => {
    return getDatasets()
        .then((datasets) => datasets.length > 0 ? axios.delete('/v1/modeldb/dataset/deleteDatasets', {
            data: { ids: datasets.map(({ id }) => id) },
        }) : Promise.resolve())
};

const createDatasetVersions = async (dataset, datasetVersions) => {
    const [{ data: { dataset: createdDataset } }] = await createDatasets([dataset]);
    const createdDatasetVersions = await Promise.all(
        datasetVersions.map((datasetVersion) => {
            return axios.post('/v1/modeldb/dataset-version/createDatasetVersion', ({
                ...datasetVersion,
                dataset_id: createdDataset.id,
            }))
        })
    );
    return {
        datasetId: createdDataset.id,
        datasetVersions: createdDatasetVersions.map(({ data: { dataset_version } }) => dataset_version),
    };
};
const createDatasetVersion = async (dataset, datasetVersion) => {
    const datasetVersions = [datasetVersion];
    const [{ data: { dataset: createdDataset } }] = await createDatasets([dataset]);
    return await Promise.all(
        datasetVersions.map((datasetVersion) => axios.post('/v1/modeldb/dataset-version/createDatasetVersion', ({
            ...datasetVersion,
            dataset_id: createdDataset.id,
        })))
    )
    .then(([{ data: { dataset_version } }]) => ({ datasetId: dataset_version.dataset_id, datasetVersionId: dataset_version.id }));
};

const createOrganization = async ({ name }) => {
    return axios.post('/v1/uac-proxy/organization/setOrganization', {
        organization: {
            name,
        }
    });
};
const createOrganizations = async (organizationSettings) => {
    return await Promise.all(organizationSettings.map(orgSettings => createOrganization(orgSettings)));
};
const getOrganizations = async () => {
    const response = await axios.get('/v1/uac-proxy/organization/listMyOrganizations');
    return (response.data.organizations || []);
};
const R = require('ramda');
const deleteAllEntitiesFromOrganizations = async ({ getAllEntities, deleteEntitiesByIds }) => {
    const organizations = await getOrganizations();
    const allEntitiesFromAllOrganizations = await Promise.all(organizations.map(({ name }) => getAllEntities({ workspace_name: name }))).then(R.chain(R.identity));
    return await deleteEntitiesByIds(allEntitiesFromAllOrganizations.map(({ id }) => id));
};
const deleteAllOrganizations = async () => {
    const organizations = await getOrganizations();
    return Promise.all(
        organizations
            .map(({ id }) => axios.post('/v1/uac-proxy/organization/deleteOrganization', { org_id: id }))
    );
};
const deleteAllEntitiesFromAllWorkspaces = async ({ getAllEntities, deleteEntitiesByIds }) => {
    const projectsFromPersonalWorkspace = await getAllEntities();
    await deleteEntitiesByIds(projectsFromPersonalWorkspace.map(({ id }) => id)); // delete from a user workspace
    await deleteAllEntitiesFromOrganizations({ getAllEntities, deleteEntitiesByIds });
};

module.exports = {
    getUserByEmail,
    updateUsername,
    
    deleteAllProjects,
    deleteProjectsByIds,
    getProjects,
    createProjects,
    createExperiments,
    createExperimentRuns,
    createExperimentRun,

    createDatasets,
    deleteAllDatasets,
    getDatasets,
    deleteDatasetsByIds,
    
    createDatasetVersions,
    createDatasetVersion,

    createOrganization,
    createOrganizations,
    deleteAllOrganizations,
    deleteAllEntitiesFromOrganizations,
    deleteAllEntitiesFromAllWorkspaces,
};
