const axios = require('axios');
const { deleteAllProjects, createProjects } = require('../../helpers/userData');

const createExperiments = async (experiments) => {
    return Promise.all(experiments.map((experiment) => axios.post('/v1/modeldb/experiment/createExperiment', experiment)));
};

const createProjectExperiments = async (project, experiments) => {
    const [{ data: { project: createdProject } }] = await createProjects([project]);
    return createExperiments(experiments.map((experiment) => ({
        project_id: createdProject.id,
        ...experiment,
    }))).then(([{ data: { experiment: { project_id } } }]) => project_id);
};

module.exports = {
    deleteAllProjects,
    createProjectExperiments,
};
