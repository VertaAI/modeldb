import { matchPath } from 'react-router';

export interface IRouteSettings<T> {
  getPath: () => string;
  getRedirectPath?: (options: T) => string;
}
export interface IRoute<T> {
  getPath: () => string;
  getRedirectPath: (options: T) => string;
  getMatch(location: string): T | null;
}

export default function makeRoute<T>({
  getPath,
  getRedirectPath,
}: IRouteSettings<T>): IRoute<T> {
  return {
    getPath,
    getRedirectPath: getRedirectPath || getPath,
    getMatch: (location: string) => {
      const path = getPath();
      const match = matchPath<T>(location, { path, exact: true });
      if (match) {
        return match.params;
      }
      return null;
    },
  };
}
