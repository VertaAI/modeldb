const apiAddress = `${process.env.REACT_APP_BACKEND_API_PROTOCOL}://${process.env.REACT_APP_BACKEND_API_DOMAIN}${
  process.env.REACT_APP_BACKEND_API_PORT ? `:${process.env.REACT_APP_BACKEND_API_PORT}` : ''
}`;

export const PROJECTS_LIST = {
  endpoint: `${apiAddress}/v1/project/getProjects`,
  method: 'get'
};

export const EXPERIMENT_RUNS = {
  body: (id: string) => JSON.stringify({ project_id: id }),
  endpoint: `${apiAddress}/v1/experiment-run/getExperimentRunsInProject`,
  method: 'get'
};

export const MODEL_RECORD = {
  body: (mid: string) => JSON.stringify({ id: mid }),
  endpoint: `${apiAddress}/v1/experiment/getExperiment`,
  method: 'get'
};
