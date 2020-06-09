import * as R from 'ramda';
import { RouteComponentProps } from 'react-router';
import { DeepPartial } from 'redux';

export function makeRouterMockProps<P>(
  params: P,
  defaultProps: DeepPartial<RouteComponentProps<P, {}>> = {}
): RouteComponentProps<P, {}> {
  const mockProps: RouteComponentProps<P, {}> = R.mergeDeepRight(
    {
      location: {
        pathname: 'Pathname',
        search: 'Search',
        state: 'LocationState',
        key: 'LocationKey',
        hash: 'hash',
      },
      history: {
        listen: jest.fn(),
        location: {
          pathname: 'pathname',
          search: '',
          state: '',
          key: '',
          hash: '',
        },
        push: jest.fn(),
      },
      match: {
        params,
        isExact: false,
        url: '',
        path: '',
      },
      staticContext: {} as any,
    },
    defaultProps
  ) as any;
  return mockProps;
}
