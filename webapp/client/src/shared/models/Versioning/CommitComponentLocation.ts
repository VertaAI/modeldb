import * as R from 'ramda';

import { Brand } from '../../utils/Brand';
import { IFolderElement, CommitTag } from './RepositoryData';

declare const CommitComponentLocationSymbol: unique symbol;
export type CommitComponentLocation = Array<
  Brand<string, 'commitComponentLocation', typeof CommitComponentLocationSymbol>
>;

export const makeRoot = () => [] as CommitComponentLocation;

export const isRoot = (location: CommitComponentLocation) => {
  return R.equals(location, makeRoot());
};

export const makeFromNames = (names: Array<IFolderElement['name']>) => {
  return (names as any) as CommitComponentLocation;
};

export const makeFromPathname = (pathname: string): CommitComponentLocation => {
  return pathname === '' || pathname === '/'
    ? makeRoot()
    : (pathname.split('/') as CommitComponentLocation).filter(Boolean);
};

export const join = (
  names: Array<IFolderElement['name'] | CommitTag>,
  path: CommitComponentLocation
): CommitComponentLocation => {
  return path.concat((names as any).filter(Boolean) as CommitComponentLocation);
};

export const add = (
  name: IFolderElement['name'],
  path: CommitComponentLocation
): CommitComponentLocation => {
  return join([name], path);
};

export const prepend = (
  name: IFolderElement['name'],
  location: CommitComponentLocation
) => {
  return [name, ...location] as CommitComponentLocation;
};

export const addAsLocationQueryParams = (
  location: CommitComponentLocation,
  pathname: string
): string => {
  return `${pathname}?${location.map(name => `location=${name}`).join('&')}`;
};

export const addAsLocationPrefixQueryParams = (
  location: CommitComponentLocation,
  pathname: string
): string => {
  return `${pathname}?${location
    .map(name => `location_prefix=${name}`)
    .join('&')}`;
};

export const toPathname = (location: CommitComponentLocation) => {
  return `${location.join('/')}`;
};

export const toArray = (location: CommitComponentLocation): string[] =>
  location;

export const getLocationByIndex = (
  location: CommitComponentLocation,
  i: number
) => {
  return location.slice(0, i + 1);
};
