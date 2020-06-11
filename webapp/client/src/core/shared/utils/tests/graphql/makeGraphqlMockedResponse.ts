import { DocumentNode, GraphQLError } from 'graphql';

import { MockedResponse } from '@apollo/react-testing';

function makeGraphqlMockedResponse<Data, Variables>(
  query: DocumentNode,
  options: {
    variables: Variables;
    getResult: () => { data: Data; errors?: GraphQLError[] };
  }
): MockedResponse {
  const response: MockedResponse = {
    request: {
      query,
      variables: options.variables,
    },
    result: options.getResult(),
  };
  return response;
}

export default makeGraphqlMockedResponse;
