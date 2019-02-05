export const PROJECTS_LIST = {
  endpoint: `${process.env.REACT_APP_PROTOCOL}://${process.env.REACT_APP_DOMAIN}:${process.env.REACT_APP_PORT}/v1/example/getProjects`,
  method: 'post'
};

export const EXPERIMENT_RUNS = {
  body: (id: string) => JSON.stringify({ project_id: id }),
  endpoint: `${process.env.REACT_APP_PROTOCOL}://${process.env.REACT_APP_DOMAIN}:${
    process.env.REACT_APP_PORT
  }/v1/example/getExperimentRunsInProject`,
  method: 'post'
};

export const MODEL_RECORD = {
  body: (mid: string) => JSON.stringify({ id: mid }),
  endpoint: `${process.env.REACT_APP_PROTOCOL}://${process.env.REACT_APP_DOMAIN}:${process.env.REACT_APP_PORT}/v1/example/getExperiment`,
  method: 'post'
};
