import qs from 'query-string';
import { matchPath, generatePath } from 'react-router';

export interface IRouteSettings<
  T,
  QueryParams extends Record<any, any> | undefined = undefined
> {
  getPath: () => string;
}
export interface IRoute<
  T,
  QueryParams extends Record<any, any> | undefined = undefined
> {
  getPath: () => string;
  getRedirectPath: (params: T) => string;
  getRedirectPathWithQueryParams: (opts: {
    params: T;
    queryParams: QueryParams;
  }) => string;
  parseQueryParams: (location: string) => Partial<QueryParams> | undefined;
  getMatch(location: string, exact?: boolean): T | null;
}

export default function makeRoute<
  T extends Record<any, any>,
  QueryParams extends Record<any, any> | undefined = undefined
>({ getPath }: IRouteSettings<T, QueryParams>): IRoute<T, QueryParams> {
  return {
    getPath,
    getRedirectPath: (params: T) => {
      const path = getPath();
      return generatePath(path, params);
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
      const match = matchPath<T>(location, { path, exact });
      if (match) {
        return match.params;
      }
      return null;
    },
  };
}
