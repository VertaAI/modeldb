import qs from 'query-string';
import { matchPath, generatePath } from 'react-router';

import * as PathBuilder from './pathBuilder';

type AllowedUserType = 'unauthorized' | 'authorized' | 'any';

export interface IRouteSettings<
  Params,
  QueryParams extends Record<any, any> | undefined = undefined
> {
  getPath: () => string;
  allowedUserType: AllowedUserType;
}
export interface IRoute<
  Params,
  QueryParams extends Record<any, any> | undefined = undefined,
  GetRedirectPathOptions = Params
> {
  allowedUserType: AllowedUserType;
  getPath: () => string;
  getRedirectPath: (options: GetRedirectPathOptions) => string;
  getRedirectPathWithQueryParams: (opts: {
    params: Params;
    queryParams: Partial<QueryParams>;
  }) => string;
  parseQueryParams: (location: string) => Partial<QueryParams> | undefined;
  getMatch(location: string, exact?: boolean): Params | null;
}

export const makeRouteFromPath = <Path extends PathBuilder.IPath<any, any>>({
  getPath,
  ...restSettings
}: { getPath: () => Path } & Omit<IRouteSettings<any, any>, 'getPath'>): IRoute<
  PathBuilder.GetParams<Path>,
  PathBuilder.GetQueryParams<Path>
> => {
  return makeRoute({
    getPath: () => getPath().value,
    ...restSettings,
  });
};

export default function makeRoute<
  Params extends Record<any, any>,
  QueryParams extends Record<any, any> | undefined = undefined,
  GetRedirectPathOptions extends Record<any, any> = Params
>({
  getPath,
  allowedUserType,
}: IRouteSettings<Params, QueryParams>): IRoute<Params, QueryParams> {
  return {
    allowedUserType,
    getPath,
    getRedirectPath: (options: GetRedirectPathOptions) => {
      const path = getPath();
      return generatePath(path, { ...options });
    },
    getRedirectPathWithQueryParams({ params, queryParams }) {
      const queryParamsInPath = Object.entries((queryParams || {}) as Record<
        any,
        any
      >)
        .map(([key, value]) => `${key}=${value}`)
        .join('&');
      return `${this.getRedirectPath(params)}?${queryParamsInPath}`;
    },
    parseQueryParams: (search: string) => {
      return (qs.parseUrl(search).query || undefined) as any;
    },
    getMatch: (location: string, exact: boolean = true) => {
      const path = getPath();
      const match = matchPath<Params>(location, { path, exact });
      if (match) {
        return match.params;
      }
      return null;
    },
  };
}
