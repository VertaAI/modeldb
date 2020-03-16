import * as R from 'ramda';

import { Brand } from '../../utils/Brand';
import { IFolderElement, CommitTag } from './RepositoryData';

declare const DataLocationSymbol: unique symbol;
export type DataLocation = Array<
  Brand<string, 'dataLocation', typeof DataLocationSymbol>
>;

export const makeRoot = () => [] as DataLocation;

export const isRoot = (location: DataLocation) => {
  return R.equals(location, makeRoot());
};

export const makeFromNames = (names: Array<IFolderElement['name']>) => {
  return (names as any) as DataLocation;
};

export const makeFromPathname = (pathname: string): DataLocation => {
  return pathname === '' || pathname === '/'
    ? makeRoot()
    : (pathname.split('/') as DataLocation).filter(Boolean);
};

export const join = (
  names: Array<IFolderElement['name'] | CommitTag>,
  path: DataLocation
): DataLocation => {
  return path.concat((names as any).filter(Boolean) as DataLocation);
};

export const add = (
  name: IFolderElement['name'],
  path: DataLocation
): DataLocation => {
  return join([name], path);
};

export const prepend = (
  name: IFolderElement['name'],
  location: DataLocation
) => {
  return [name, ...location] as DataLocation;
};

export const addAsLocationQueryParams = (
  location: DataLocation,
  pathname: string
): string => {
  return `${pathname}?${location.map(name => `location=${name}`).join('&')}`;
};

export const addAsLocationPrefixQueryParams = (
  location: DataLocation,
  pathname: string
): string => {
  return `${pathname}?${location
    .map(name => `location_prefix=${name}`)
    .join('&')}`;
};

export const toPathname = (location: DataLocation) => {
  return `${location.join('/')}`;
};

export const getLocationByIndex = (location: DataLocation, i: number) => {
  return location.slice(0, i + 1);
};
