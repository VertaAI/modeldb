import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { Branch } from 'core/shared/models/Versioning/RepositoryData';
import resultToCommunicationWithData from 'core/shared/utils/graphql/queryResultToCommunicationWithData';
import { USER_FRAGMENT, convertUser } from 'core/shared/graphql/User/User';

import * as Types from './graphql-types/CommitsHistory';
import { ICommitView } from '../types';

export { Types };

const COMMITS_HISTORY = gql`
  query CommitsHistory($repositoryId: ID!, $commitReference: CommitReference!) {
    repository(id: $repositoryId) {
      id
      log(commit: $commitReference) {
        commits {
          id
          message
          date
          author {
            ...UserData
          }
        }
      }
    }
  }
  ${USER_FRAGMENT}
`;

export const useCommitsHistoryQuery = ({
  repositoryId,
  branch,
}: {
  branch: Branch;
  repositoryId: IRepository['id'];
}) => {
  const res = useQuery<Types.CommitsHistory, Types.CommitsHistoryVariables>(
    COMMITS_HISTORY,
    {
      variables: {
        repositoryId,
        commitReference: {
          branch,
        },
      },
    }
  );

  return resultToCommunicationWithData(data => {
    if (data.repository && data.repository.log) {
      const commits: ICommitView[] = data.repository.log.commits.map(
        serverCommit => ({
          dateCreated: new Date(Number(serverCommit.date)),
          message: serverCommit.message,
          sha: serverCommit.id,
          author: convertUser(serverCommit.author),
        })
      );
      return commits;
    }
  }, res);
};
