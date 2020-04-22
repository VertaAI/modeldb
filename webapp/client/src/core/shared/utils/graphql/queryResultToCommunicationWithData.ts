import { ApolloError } from 'apollo-boost';
import { QueryResult, MutationResult } from 'react-apollo';

import { HttpError, AppError } from 'core/shared/models/Error';

import {
  ICommunication,
  requestingCommunication,
  makeErrorCommunication,
  successfullCommunication,
  initialCommunication,
} from '../redux/communication';

const resultToCommunicationWithData = <T, R>(
  convert: (res: T) => R,
  queryResult: QueryResult<T> | MutationResult<T>
): {
  communication: ICommunication;
  data: R | undefined;
} => {
  if (queryResult.loading) {
    return {
      communication: requestingCommunication,
      data: queryResult.data && convert(queryResult.data),
    };
  }
  if (queryResult.error) {
    return {
      communication: makeErrorCommunication(
        graphqlErrorToAppError(queryResult.error)
      ),
      data: queryResult.data && convert(queryResult.data),
    };
  }
  if (queryResult.data) {
    return {
      communication: successfullCommunication,
      data: convert(queryResult.data),
    };
  }
  return { communication: initialCommunication, data: undefined };
};

export const resultToCommunicationWithSavingDataOnRefetching = <T, R>(
  convert: (res: T) => R,
  queryResult: QueryResult<T> | MutationResult<T>
): {
  communication: ICommunication;
  data: R | undefined;
} => {
  const res = resultToCommunicationWithData(convert, queryResult);
  if (!res.data && queryResult.data) {
    return { ...res, data: convert(queryResult.data) };
  }
  return res;
};

export const mutationResultToCommunication = (
  result: MutationResult
): ICommunication => resultToCommunicationWithData(() => {}, result).communication;

const graphqlErrorToAppError = (apolloError: ApolloError): AppError => {
  const sanitizedMessage = apolloError.message
    .replace(/GraphQL\s+error:/i, '')
    .replace(/\s+.+?:/, '')
    .trim();
  return new HttpError({
    status: 404,
    isMessageUserFriendly: true,
    message: sanitizedMessage,
  });
};

export default resultToCommunicationWithData;
