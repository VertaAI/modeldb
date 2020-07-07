import {
  IntrospectionFragmentMatcher,
  InMemoryCache,
  DefaultOptions,
} from 'apollo-boost';
import introspectionQueryResultData from 'fragmentTypes.json';

const fragmentMatcher = new IntrospectionFragmentMatcher({
  introspectionQueryResultData: introspectionQueryResultData.data,
});
export const makeCache = () =>
  new InMemoryCache({
    fragmentMatcher,
  });

export const defaultOptions: DefaultOptions = {
  watchQuery: {
    fetchPolicy: 'network-only',
  },
};
