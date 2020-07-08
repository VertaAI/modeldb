import { ApolloError } from 'apollo-boost';

import { AppError, AppGraphQLError } from 'shared/models/Error';

import {
  isServerError,
  isServerParseError,
  getHttpErrorFromServerError,
} from './apolloError';
import normalizeError from '../normalizeError';

const apolloErrorToAppError = (apolloError: ApolloError): AppError => {
  if (
    apolloError.networkError &&
    (isServerError(apolloError.networkError) ||
      isServerParseError(apolloError.networkError))
  ) {
    return getHttpErrorFromServerError(apolloError.networkError);
  }

  if ((apolloError.graphQLErrors || []).length > 0) {
    const sanitizedMessage = apolloError.graphQLErrors[0].message
      .replace(/GraphQL\s+error:\s*/i, '')
      .trim();
    return new AppGraphQLError({
      message: sanitizedMessage,
    });
  }

  return normalizeError(new Error('Unknown error'));
};

export default apolloErrorToAppError;
