import * as R from 'ramda';

import { AppError } from 'core/shared/models/Error';
import { RecordFromUnion } from 'core/shared/utils/types';

import { initialCommunication } from './communicationStates';
import { CommunicationState, ICommunication } from './types';

export type ICommunicationMatchers<
  Error extends AppError<any>,
  Result
> = RecordFromUnion<
  CommunicationState,
  {
    notAsked: () => Result;
    requesting: () => Result;
    error: (error: Error) => Result;
    success: () => Result;
  }
>;

const matchCommunication = <Error extends AppError<any>, Result>(
  communication: ICommunication<Error>,
  matchers: ICommunicationMatchers<Error, Result>
): Result => {
  if (R.equals(communication, initialCommunication as any)) {
    return matchers.notAsked();
  }
  if (communication.isRequesting) {
    return matchers.requesting();
  }
  if (communication.error) {
    return matchers.error(communication.error);
  }
  return matchers.success();
};

export default matchCommunication;
