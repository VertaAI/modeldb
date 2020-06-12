import { convertServerDiffsToClient } from 'core/services/serverModel/Versioning/CompareCommits/converters';

export const convertGraphqlDiffs = (diffs: string[] | null) => {
  return convertServerDiffsToClient(
    (diffs || []).map(diff => JSON.parse(diff))
  );
};
