import gql from 'graphql-tag';
import { useQuery } from 'react-apollo';
import { useHistory } from 'react-router';
import { useCallback } from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IWorkspace } from 'core/shared/models/Workspace';
import {
  IPaginationSettings,
  IPagination,
} from 'core/shared/models/Pagination';
import routes from 'core/shared/routes';
import resultToCommunicationWithData from 'core/shared/utils/graphql/queryResultToCommunicationWithData';
import { USER_FRAGMENT } from 'core/shared/graphql/User/User';
import { WORKSPACE_FRAGMENT } from 'core/shared/graphql/Workspace';

import * as ServerTypes from './graphql-types/Repositories';
import { convertRepository } from '../converters/convertRepository';

const REPOSITORIES = gql`
  query Repositories($workspaceName: String!, $pagination: PaginationQuery!) {
    workspace(name: $workspaceName) {
      ...WorkspaceData
      repositories(query: { pagination: $pagination }) {
        repositories {
          name
          id
          dateCreated
          dateUpdated
          labels
          owner {
            ...UserData
          }
          collaborators {
            __typename
            ... on UserCollaborator {
              user {
                ...UserData
              }
              type
              canDeploy
            }
            ... on TeamCollaborator {
              team {
                id
                name
              }
              type
              canDeploy
            }
          }
          allowedActions {
            create
            update
            delete
          }
        }
        pagination {
          totalRecords
        }
      }
    }
  }
  ${USER_FRAGMENT}
  ${WORKSPACE_FRAGMENT}
`;

const paginationSettings: IPaginationSettings = {
  currentPage: 0,
  pageSize: 10,
};

export const useRepositoriesQuery = ({
  workspace,
}: {
  workspace: IWorkspace;
}) => {
  const currentPage = usePaginationPageFromURL();

  const res = useQuery<
    ServerTypes.Repositories,
    ServerTypes.RepositoriesVariables
  >(REPOSITORIES, {
    notifyOnNetworkStatusChange: true,
    variables: {
      workspaceName: workspace.name,
      pagination: { page: currentPage + 1, limit: paginationSettings.pageSize },
    },
  });

  const communicationWithData = resultToCommunicationWithData(
    data => convertResponse(currentPage, data, workspace),
    res
  );

  const onChangeCurrentPage = useChangeCurrentPage({
    workspaceName: workspace.name,
  });

  return {
    ...communicationWithData,
    onChangeCurrentPage,
    refetch: res.refetch,
  };
};

const useChangeCurrentPage = ({
  workspaceName,
}: {
  workspaceName: IWorkspace['name'];
}) => {
  const history = useHistory();
  const onChangeCurrentPage = useCallback((page: number) => {
    if (page !== 0) {
      history.push(
        routes.repositories.getRedirectPathWithQueryParams({
          params: { workspaceName },
          queryParams: { page: String(page + 1) },
        })
      );
    } else {
      history.push(
        routes.repositories.getRedirectPath({
          workspaceName,
        })
      );
    }
  }, []);

  return onChangeCurrentPage;
};

const usePaginationPageFromURL = () => {
  const queryParams = routes.repositories.parseQueryParams(
    window.location.href
  );
  if (queryParams && queryParams.page) {
    return Number(queryParams.page) - 1;
  }
  return 0;
};

const convertResponse = (
  currentPage: number,
  res: ServerTypes.Repositories | undefined,
  currentWorkspace: IWorkspace
): { repositories: IRepository[]; pagination: IPagination } | undefined => {
  if (res && res.workspace) {
    const repositories: IRepository[] = res.workspace.repositories.repositories.map(
      serverRepository => convertRepository(serverRepository, currentWorkspace)
    );
    return {
      repositories,
      pagination: {
        currentPage,
        pageSize: paginationSettings.pageSize,
        totalCount: res.workspace.repositories.pagination.totalRecords,
      },
    };
  }
  return undefined;
};
