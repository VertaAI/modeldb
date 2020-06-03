import * as R from 'ramda';

import { IBlob } from 'core/shared/models/Versioning/Blob/Blob';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import {
  IFolder,
  ISubFolderElement,
  IBlobFolderElement,
} from 'core/shared/models/Versioning/RepositoryData';

type IOutputData = IOutputBlob<any> | IOutputFolder<any, any>;
interface IOutputBlob<Name extends string> {
  type: 'blob';

  asFolderElement: IBlobFolderElement;
  asDataElement: IBlob;

  name: Name;
  location: CommitComponentLocation.CommitComponentLocation;
}
interface IOutputFolder<
  Name extends string,
  Elements extends Record<any, ArrayOutputData<any>>
> {
  type: 'folder';

  elements: Elements;

  asFolderElement: ISubFolderElement;
  asDataElement: IFolder;

  name: Name;
  location: CommitComponentLocation.CommitComponentLocation;
}
interface IRoot<Records extends Record<any, ArrayOutputData<any>>> {
  elements: Records;
  asDataElement: IFolder;
}

type ArrayOutputData<T extends IOutputData> = T[];

export const root = <T extends ArrayOutputData<any>>(
  elements: T
): IRoot<{ [K in T[number]['name']]: Extract<T[number], { name: K }> }> => {
  return {
    elements: R.fromPairs(elements.map(e => [e.name, e])) as any,
    asDataElement: {
      type: 'folder',
      blobs: elements
        .filter(e => e.type === 'blob')
        .map(e => e.asFolderElement),
      subFolders: elements
        .filter(e => e.type === 'folder')
        .map(e => e.asFolderElement),
    },
  };
};

export const folder = <Name extends string, T extends ArrayOutputData<any>>(
  name: Name,
  { createdByCommitSha }: { createdByCommitSha: string },
  elements: T
): IOutputFolder<
  Name,
  { [K in T[number]['name']]: Extract<T[number], { name: K }> }
> => {
  const location = CommitComponentLocation.makeFromNames([name as any]);

  return {
    type: 'folder',
    name,

    asFolderElement: {
      name: name as any,
      createdByCommitSha,
      type: 'folder',
    },
    asDataElement: {
      type: 'folder',
      blobs: elements
        .filter(e => e.type === 'blob')
        .map(e => e.asFolderElement),
      subFolders: elements
        .filter(e => e.type === 'folder')
        .map(e => e.asFolderElement),
    },

    location,

    elements: R.fromPairs(
      elements
        .map(elem => nestFolderLocation(location, elem))
        .map(e => [e.name, e])
    ) as any,
  };
};

const nestFolderLocation = (
  newParentLocation: CommitComponentLocation.CommitComponentLocation,
  element: IOutputData
): IOutputData => {
  const newLocation = CommitComponentLocation.add(
    element.name,
    newParentLocation
  );
  if (element.type === 'blob') {
    return {
      ...element,
      location: newLocation,
    };
  }
  return {
    ...element,
    elements: R.map(
      elem => nestFolderLocation(newLocation, elem),
      element.elements
    ),
    location: newLocation,
  } as IOutputData;
};

export const blob = <Name extends string>(
  name: Name,
  { createdByCommitSha }: { createdByCommitSha: string },
  data: IBlob['data']
): IOutputBlob<Name> => {
  const location = CommitComponentLocation.makeFromNames([name as any]);
  return {
    name: name as any,
    asDataElement: { data, type: 'blob' },
    asFolderElement: {
      createdByCommitSha,
      name: name as any,
      type: 'blob',
    },
    location,
    type: 'blob',
  };
};
