import matchBy from 'core/shared/utils/matchBy';
import {
  IElementDiff,
  DiffType,
  IBlobDiff,
  elementDiffMakers,
} from 'core/shared/models/Versioning/Blob/Diff';
import matchType from 'core/shared/utils/matchType';
import { DataLocation } from 'core/shared/models/Versioning/DataLocation';

export type IServerBlobDiff<Content> = {
  location: string[];
  status: 'ADDED' | 'MODIFIED' | 'DELETED';
} & Content;

export type IServerElementDiff<Element> =
  | {
      status: 'ADDED';
      B: Element;
    }
  | {
      status: 'DELETED';
      A: Element;
    }
  | {
      status: 'MODIFIED';
      A: Element;
      B: Element;
    };

export const DiffStatus = {
  ADDED: 'ADDED',
  DELETED: 'DELETED',
  MODIFIED: 'MODIFIED',
};

export const convertServerBlobDiffToClient = <
  T extends IServerBlobDiff<any>,
  R extends IBlobDiff<any, any>
>(
  {
    convertData,
    category,
    type,
  }: {
    convertData: (serverDiff: T, context: { diffType: DiffType }) => R['data'];
  } & Pick<R, 'category' | 'type'>,
  serverDiff: T
): R => {
  const { location, status } = serverDiff;
  const diffType = serverDiffTypeToClient(status);
  return {
    diffType,
    category,
    type,
    location: location as DataLocation,
    data: convertData(serverDiff, { diffType }),
  } as R;
};

export const convertServerElementDiffToClient = <
  T extends IServerElementDiff<D>,
  D,
  B
>(
  convertData: (serverData: D) => B,
  serverDiff: T
): IElementDiff<B> => {
  return matchBy(serverDiff as IServerElementDiff<D>, 'status')({
    ADDED: ({ B }) => elementDiffMakers.added(convertData(B)),
    DELETED: ({ A }) => elementDiffMakers.deleted(convertData(A)),
    MODIFIED: ({ A, B }) =>
      elementDiffMakers.modified(convertData(A), convertData(B)),
  });
};

export const serverDiffTypeToClient = (
  serverDiffType: IServerElementDiff<any>['status']
): DiffType => {
  return matchType(
    {
      ADDED: () => 'added',
      DELETED: () => 'deleted',
      MODIFIED: () => 'modified',
    },
    serverDiffType
  );
};
