import { AppError } from 'shared/models/Error';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'shared/utils/redux/communication';
import {
  IProjectCreationSettings,
  Project,
  projectAlreadyExistsError,
} from 'shared/models/Project';

export interface IProjectCreationState {
  communications: {
    creatingProject: ICommunication<AppError<typeof projectAlreadyExistsError>>;
  };
}

export const createProjectActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@projectCreation/CREACT_PROJECT_REQUEST',
  SUCCESS: '@@projectCreation/CREACT_PROJECT_SUCСESS',
  FAILURE: '@@projectCreation/CREACT_PROJECT_FAILURE',
});
export type ICreateProjectActionTypes = MakeCommunicationActions<
  typeof createProjectActionTypes,
  {
    request: { settings: IProjectCreationSettings };
    success: { project: Project };
  }
>;
export enum resetCreateProjectCommunicationActionTypes {
  RESET_CREATE_PROJECT_COMMUNICATION = '@@projectCreation/RESET_CREATE_PROJECT_COMMUNICATION',
}
export interface IResetCreateProjectCommunication {
  type: resetCreateProjectCommunicationActionTypes.RESET_CREATE_PROJECT_COMMUNICATION;
}

export type FeauterAction = ICreateProjectActionTypes;
