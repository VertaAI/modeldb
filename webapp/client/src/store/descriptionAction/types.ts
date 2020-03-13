import { AppError } from 'core/shared/models/Error';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'core/shared/utils/redux/communication';

export interface IDescriptionActionState {
  communications: {
    addingDesc: ICommunication;
    editingDesc: ICommunication;
  };
}

export const addOrEditDescActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@descAction/ADD_EDIT_DESC_REQUEST',
  SUCCESS: '@@descAction/ADD_EDIT_DESC_SUCСESS',
  FAILURE: '@@descAction/ADD_EDIT_DESC_FAILURE',
});
export type IAddOrEditDescActions = MakeCommunicationActions<
  typeof addOrEditDescActionTypes,
  {
    request: { id: string };
    success: { id: string; data: { description: string } };
    failure: { id: string; error: AppError };
  }
>;

export type FeatureAction = IAddOrEditDescActions;
