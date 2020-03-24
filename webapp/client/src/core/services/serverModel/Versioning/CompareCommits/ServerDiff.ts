import matchBy from "core/shared/utils/matchBy";
import { IElementDiff, DiffType } from "core/shared/models/Versioning/Blob/Diff";
import matchType from "core/shared/utils/matchType";

export type IServerBlobDiff<Content> = {
    location: string[];
    status: 'ADDED' | 'MODIFIED' | 'DELETED';
  } & Content;
  
export type IServerElementDiff<Element> =
    {
      status: 'ADDED';
      B: Element;
    } |
    {
      status: 'DELETED';
      A: Element;
    } |
    {
      status: 'MODIFIED';
      A: Element;
      B: Element;
    };

export const DiffStatus = {
    ADDED: 'ADDED',
    DELETED: 'DELETED',
    MODIFIED: 'MODIFIED'
};

export const convertServerElementDiffToClient = <T extends IServerElementDiff<any>, R extends IElementDiff<B>, B>(
    convertData: (serverData: T extends IServerElementDiff<infer D> ? D : never) => B, serverDiff: T): R => {
    return matchBy(serverDiff as IServerElementDiff<any>, 'status')({
      ADDED: ({ B }) => ({ diffType: 'added', B: convertData(B) }) as any,
      DELETED: ({ A }) => ({ diffType: 'deleted', A: convertData(A) }) as any,
      MODIFIED: ({ A, B }) => ({ diffType: 'updated', A: convertData(A), B: convertData(B) }) as any,
    });
  };

export const serverDiffTypeToClient = (serverDiffType: IServerElementDiff<any>['status']): DiffType => {
  return matchType({
    ADDED: () => 'added',
    DELETED: () => 'deleted',
    MODIFIED: () => 'updated',
  }, serverDiffType);
};
