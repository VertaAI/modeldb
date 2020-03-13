type ProjectId = string;
type ExperimentRunId = string;
export type EntityId = ProjectId | ExperimentRunId;

export type CommentEntityType = 'experimentRun' | 'project';

export interface IComment {
  id: string;
  dateTime: Date;
  message: string;
}
