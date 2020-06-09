import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import { USER_FRAGMENT, convertUser } from 'shared/graphql/User/User';
import { IRepository } from 'shared/models/Versioning/Repository';
import { ICommit } from 'shared/models/Versioning/RepositoryData';
import resultToCommunicationWithData from 'shared/utils/graphql/queryResultToCommunicationWithData';
import { convertGraphqlDiffs } from 'shared/graphql/Versioning/Diff';

import * as Types from './graphql-types/COMMIT_DETAILS';
import { ICommitDetails } from '../types';

const COMMIT_DETAILS = gql`
  query COMMIT_DETAILS($repositoryId: ID!, $commitSha: ID!) {
    repository(id: $repositoryId) {
      id
      commit(id: $commitSha) {
        id
        date
        message
        author {
          ...UserData
        }
        asDiff {
          parent
          diff
        }
        runs(query: {}) {
          runs {
            id
            name
            experiment {
              id
              name
            }
            project {
              id
              name
            }
          }
        }
      }
    }
  }
  ${USER_FRAGMENT}
`;

export const useCommitDetailsQuery = ({
  repositoryId,
  commitSha,
}: {
  repositoryId: IRepository['id'];
  commitSha: ICommit['sha'];
}) => {
  const res = useQuery<Types.COMMIT_DETAILS, Types.COMMIT_DETAILSVariables>(
    COMMIT_DETAILS,
    {
      variables: { repositoryId, commitSha },
    }
  );

  return resultToCommunicationWithData(serverRes => {
    if (serverRes.repository && serverRes.repository.commit) {
      const serverCommit = serverRes.repository.commit;
      const serverAsDiff = serverRes.repository.commit.asDiff;
      const res: ICommitDetails = {
        commit: {
          author: convertUser(serverCommit.author),
          dateCreated: new Date(Number(serverCommit.date)),
          message: serverCommit.message,
          sha: commitSha,
          parentSha: serverAsDiff ? serverAsDiff.parent : undefined,
        },
        experimentRuns: serverRes.repository.commit.runs.runs,
        diffs: convertGraphqlDiffs(serverAsDiff && serverAsDiff.diff),
      };
      return res;
    }
  }, res);
};
