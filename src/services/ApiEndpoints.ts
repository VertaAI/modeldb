export const PROJECTS_LIST = { endpoint: 'http://localhost:8080/v1/example/getProjects', method: 'post' };

export const EXPERIMENT_RUNS = {
  body: (id: string) => JSON.stringify({ project_id: id }),
  endpoint: 'http://localhost:8080/v1/example/getExperimentRunsInProject',
  method: 'post'
};

export const MODEL_RECORD = {
  body: (mid: string) => JSON.stringify({ id: mid }),
  endpoint: 'http://localhost:8080/v1/example/getExperiment',
  method: 'post'
};
