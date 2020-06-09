import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { resultToCommunicationWithSavingDataOnRefetching } from 'core/shared/utils/graphql/queryResultToCommunicationWithData';
import { IFullCommitComponentLocationComponents } from 'core/shared/models/Versioning/RepositoryData';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { USER_FRAGMENT } from 'core/shared/graphql/User/User';
import { getCommitReference } from 'core/shared/graphql/Versioning/CommitReference';

import * as Types from './graphql-types/CommitWithComponent';
import { convertCommitWithComponent } from './converters';

export { Types };
export const COMMIT_WITH_COMPONENT = gql`
  query CommitWithComponent(
    $repositoryId: ID!
    $location: [String!]!
    $commitReference: CommitReference!
  ) {
    repository(id: $repositoryId) {
      id
      commitByReference(ref: $commitReference) {
        id
        message
        date
        author {
          ...UserData
        }
        getLocation(location: $location) {
          __typename
          ... on CommitFolder {
            subfolders {
              name
            }
            blobs {
              name
            }
          }
          ... on CommitBlob {
            content
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
    }
  }
  ${USER_FRAGMENT}
`;

export const useRepositoryDataQuery = ({
  repositoryId,
  fullCommitComponentLocation,
}: {
  repositoryId: IRepository['id'];
  fullCommitComponentLocation: IFullCommitComponentLocationComponents;
}) => {
  const queryRes = useQuery<
    Types.CommitWithComponent,
    Types.CommitWithComponentVariables
  >(COMMIT_WITH_COMPONENT, {
    notifyOnNetworkStatusChange: true,
    variables: {
      repositoryId,
      commitReference: getCommitReference(
        fullCommitComponentLocation.commitPointer
      ),
      location: CommitComponentLocation.toArray(
        fullCommitComponentLocation.location
      ),
    },
  });

  return {
    refetch: queryRes.refetch,
    ...resultToCommunicationWithSavingDataOnRefetching(res => {
      if (
        res.repository &&
        res.repository.commitByReference &&
        res.repository.commitByReference
      ) {
        return convertCommitWithComponent(res.repository.commitByReference);
      }
    }, queryRes),
  };
};
