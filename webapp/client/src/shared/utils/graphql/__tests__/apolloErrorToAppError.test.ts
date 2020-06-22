import { ServerError } from 'apollo-link-http-common';
import { ApolloError } from 'apollo-boost';
import { GraphQLError } from 'graphql';

import apolloErrorToAppError from '../apolloErrorToAppError';

describe('apolloErrorToAppError', () => {
  describe('when there is a graphql error', () => {
    it(`should remove "GraphQL error: " if it there is`, () => {
      const error = new ApolloError({
        graphQLErrors: [
          new GraphQLError(
            'GraphQL error: Already exists: Repository already exists in database'
          ),
        ],
      });
      expect(apolloErrorToAppError(error).message).toEqual(
        'Already exists: Repository already exists in database'
      );
    });

    it(`should not change an error if there is not "GraphQL error: "`, () => {
      const error = new ApolloError({
        graphQLErrors: [
          new GraphQLError(
            'Already exists: Repository already exists in database'
          ),
        ],
      });
      expect(apolloErrorToAppError(error).message).toEqual(
        'Already exists: Repository already exists in database'
      );
    });
  });

  describe('when there is server network error', () => {
    it(`should remove "Network error: " if it there is`, () => {
      const error = new ApolloError({
        networkError: makeServerError({
          message:
            'Network error: Response not successful: Received status code 422',
          statusCode: 422,
        }),
      });
      expect(apolloErrorToAppError(error).message).toEqual(
        'Response not successful: Received status code 422'
      );
    });

    it(`should not change an error if there is not "Network error: "`, () => {
      const error = new ApolloError({
        networkError: makeServerError({
          message:
            'Network error: Response not successful: Received status code 422',
          statusCode: 422,
        }),
      });
      expect(apolloErrorToAppError(error).message).toEqual(
        'Response not successful: Received status code 422'
      );
    });
  });
});

const makeServerError = ({
  statusCode,
  message,
}: {
  statusCode: number;
  message: string;
}): ServerError => {
  const error: ServerError = new Error(message) as ServerError;
  error.statusCode = statusCode;
  error.message = message;
  error.name = 'ServerError';
  return error;
};
