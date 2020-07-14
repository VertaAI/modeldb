import { CommitReference } from 'graphql-types/graphql-global-types';

import { CommitPointer } from 'shared/models/Versioning/RepositoryData';
import matchBy from 'shared/utils/matchBy';

export const getCommitReference = (commitPointer: CommitPointer) =>
  matchBy(commitPointer, 'type')<CommitReference>({
    branch: ({ value }) => ({ branch: value }),
    commitSha: ({ value }) => ({ commit: value }),
    tag: ({ value }) => ({ tag: value }),
  });
