export interface IRouteSettings<T> {
  getPath: () => string;
  getRedirectPath?: (options: T) => string;
}
export interface IRoute<T> {
  getPath: () => string;
  getRedirectPath: (options: T) => string;
}

export default function makeRoute<T>({
  getPath,
  getRedirectPath,
}: IRouteSettings<T>): IRoute<T> {
  return { getPath, getRedirectPath: getRedirectPath || getPath };
}
