import * as R from 'ramda';

import { AppError } from 'core/shared/models/Error';
import { RecordFromUnion } from 'core/shared/utils/types';

import { initialCommunication } from './communicationStates';
import { ICommunication, CommunicationState } from './types';

export type IRemoteData<Data, Error = AppError<any>> =
  | { type: typeof CommunicationState.notAsked }
  | { type: typeof CommunicationState.requesting }
  | { type: 'errorOrNillData'; data: { error?: Error; isNillData: boolean } }
  | { type: typeof CommunicationState.success; data: Data };

type IRemoteDataMatchers<
  Data,
  Error extends AppError<any>,
  Result
> = RecordFromUnion<
  IRemoteData<Data, Error>['type'],
  {
    notAsked: () => Result;
    requesting: () => Result;
    errorOrNillData: (data: {
      error: Error | undefined;
      isNillData: boolean;
    }) => Result;
    success: (data: Data) => Result;
  }
>;

export function matchRemoteData<Data, Error extends AppError<any>, Result>(
  communication: ICommunication<Error>,
  data: Data | undefined | null,
  matchers: IRemoteDataMatchers<Data, Error, Result>
): Result {
  if (R.equals(communication, initialCommunication as ICommunication<Error>)) {
    return matchers.notAsked();
  }
  if (communication.isRequesting) {
    return matchers.requesting();
  }
  if (communication.error || !data) {
    return matchers.errorOrNillData({
      error: communication.error as any,
      isNillData: !data,
    });
  }
  if (communication.isSuccess) {
    return matchers.success(data);
  }
  return matchers.notAsked();
}
