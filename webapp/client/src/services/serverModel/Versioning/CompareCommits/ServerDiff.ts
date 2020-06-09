import matchBy from 'core/shared/utils/matchBy';
import {
  IElementDiff,
  DiffType,
  IBlobDiff,
  elementDiffMakers,
  IArrayDiff,
  makeArrayDiff,
} from 'core/shared/models/Versioning/Blob/Diff';
import matchType from 'core/shared/utils/matchType';
import { CommitComponentLocation } from 'core/shared/models/Versioning/CommitComponentLocation';

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
    }
  | {
      status: 'CONFLICTED';
      A: Element;
      B: Element;
      C?: Element;
    };

export const DiffStatus = {
  ADDED: 'ADDED',
  DELETED: 'DELETED',
  MODIFIED: 'MODIFIED',
  CONFLICTED: 'CONFLICTED',
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
    location: location as CommitComponentLocation,
    data: convertData(serverDiff, { diffType }),
  } as R;
};

export const convertServerElementDiffToClient = <
  T extends IServerElementDiff<any>,
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
    CONFLICTED: ({ A, B, C }) =>
      elementDiffMakers.conflicted(
        convertData(A),
        convertData(B),
        C && convertData(C)
      ),
  });
};
export const convertNullableServerElementDiffToClient = <
  T extends IServerElementDiff<D>,
  D,
  B
>(
  convertData: (serverData: D) => B,
  serverDiff: T | undefined
): IElementDiff<B> | undefined => {
  return serverDiff
    ? convertServerElementDiffToClient(convertData, serverDiff)
    : undefined;
};

export const convertServerArrayDiffToClient = <
  T extends IServerElementDiff<D>,
  D,
  B
>(
  convertData: (serverData: D) => B,
  serverDiff: T[]
): IArrayDiff<B> => {
  return makeArrayDiff(
    serverDiff.map(x => convertServerElementDiffToClient(convertData, x))
  );
};
export const convertNullableServerArrayDiffToClient = <
  T extends IServerElementDiff<D>,
  D,
  B
>(
  convertData: (serverData: D) => B,
  serverDiff: T[] | undefined
): IArrayDiff<B> | undefined => {
  return serverDiff
    ? convertServerArrayDiffToClient(convertData, serverDiff)
    : undefined;
};

export const serverDiffTypeToClient = (
  serverDiffType: IServerElementDiff<any>['status']
): DiffType => {
  return matchType(
    {
      ADDED: () => 'added',
      DELETED: () => 'deleted',
      MODIFIED: () => 'modified',
      CONFLICTED: () => 'conflicted',
    },
    serverDiffType
  );
};
