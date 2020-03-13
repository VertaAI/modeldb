import { AppError } from 'core/shared/models/Error';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'core/shared/utils/redux/communication';

export interface ITagActionState {
  communications: {
    addingTag: ICommunication;
    removingTag: ICommunication;
  };
}

export const removeTagActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@tagAction/REMOVE_TAG_REQUEST',
  SUCCESS: '@@tagAction/REMOVE_TAG_SUCСESS',
  FAILURE: '@@tagAction/REMOVE_TAG_FAILURE',
});
export type IRemoveTagActions = MakeCommunicationActions<
  typeof removeTagActionTypes,
  {
    request: { id: string };
    success: { id: string; tags: string[] };
    failure: { id: string; error: AppError };
  }
>;

export const addTagActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@tagAction/ADD_TAG_REQUEST',
  SUCCESS: '@@tagAction/ADD_TAG_SUCСESS',
  FAILURE: '@@tagAction/ADD_TAG_FAILURE',
});
export type IAddTagActions = MakeCommunicationActions<
  typeof addTagActionTypes,
  {
    request: { id: string };
    success: { id: string; tags: string[] };
    failure: { id: string; error: AppError };
  }
>;

export type FeatureAction = IRemoveTagActions | IAddTagActions;
