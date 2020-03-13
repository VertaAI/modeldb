import ModelRecord from 'models/ModelRecord';

export const makeExperimentRun = ({
  id,
  name,
  projectId,
}: {
  id: string;
  name: string;
  projectId: string;
}) => {
  const experimentRun: ModelRecord = new ModelRecord();
  experimentRun.projectId = projectId;
  experimentRun.id = id;
  experimentRun.name = name;
  experimentRun.tags = [];
  return experimentRun;
};
