import * as React from 'react';

import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { IWorkspace, IUserWorkspaces } from 'models/Workspace';
import routes from 'routes';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import { withAct } from 'core/shared/utils/tests/react/helpers';
import DatasetDetailPages from '../DatasetPages/DatasetDetailPages/DatasetDetailPages';
import DatasetsPage from '../DatasetPages/DatasetsPage';
import Pages from '../index';
import NotFoundPage from '../NotFoundPage/NotFoundPage';
import ProjectDetailsPage from '../ProjectsPages/ProjectDetailsPages/ProjectDetailsPage';
import ProjectsPage from '../ProjectsPages/ProjectsPage/ProjectsPage';

const userWorkspaces: IUserWorkspaces = {
  ...userWorkspacesWithCurrentUser,
};

const defaultUserWorkspacePathname = routes.projects.getRedirectPath({
  workspaceName: userWorkspaces.user.name,
});

const makeComponent = async ({ pathname }: { pathname: string }) => {
  const data = await withAct(async () => {
    return await makeMountComponentWithPredefinedData({
      Component: Pages,
      settings: { pathname },
      predefinedData: {
        userWorkspaces,
      },
    });
  });

  await flushAllPromisesFor(data.component);

  return data;
};

const checkDisplayingPageOnRoute = async ({
  pathname,
  Component,
}: {
  pathname: string;
  Component: any;
}) => {
  const component = await makeComponent({
    pathname,
  });
  expect(component.history.location.pathname).toEqual(pathname);
  expect(component.component.find(Component).length).toEqual(1);
  expect(component.component.find(NotFoundPage).length).toEqual(0);
};

describe('Pages', () => {
  it('should redirect to the projects page if a current url is `/`', async () => {
    const { component, history } = await makeComponent({
      pathname: routes.index.getRedirectPath({}),
    });

    expect(history.location.pathname).toEqual(defaultUserWorkspacePathname);
    expect(component.find(ProjectsPage).length).toEqual(1);
  });

  describe('pages with a workspace', () => {
    it('should display projects pages', async () => {
      await checkDisplayingPageOnRoute({
        Component: ProjectsPage,
        pathname: routes.projects.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        }),
      });

      await checkDisplayingPageOnRoute({
        Component: ProjectDetailsPage,
        pathname: routes.projectSummary.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.name,
          projectId: 'projectId',
        }),
      });
    });

    it('should display datasets pages', async () => {
      await checkDisplayingPageOnRoute({
        Component: DatasetsPage,
        pathname: routes.datasets.getRedirectPath({
          workspaceName: userWorkspaces.user.name,
        }),
      });

      await checkDisplayingPageOnRoute({
        Component: DatasetDetailPages,
        pathname: routes.datasetSummary.getRedirectPath({
          workspaceName: userWorkspaces.user.name,
          datasetId: 'datasetId',
        }),
      });
    });

    it('should redirect from an existing workspace to the projects page', async () => {
      const componentWithUserWorkspace = await makeComponent({
        pathname: routes.workspace.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        }),
      });
      expect(componentWithUserWorkspace.history.location.pathname).toEqual(
        defaultUserWorkspacePathname
      );
      expect(
        componentWithUserWorkspace.component.find(ProjectsPage).length
      ).toEqual(1);
    });

    it('should show the 404 page if a workspace doesn`t exist', async () => {
      const pathname = routes.workspace.getRedirectPath({
        workspaceName: 'unknown-workspace' as IWorkspace['name'],
      });
      const { component, history } = await makeComponent({ pathname });
      expect(history.location.pathname).toEqual(pathname);
      expect(component.find(NotFoundPage).length).toEqual(1);
    });

    it('should show the 404 page if a workspace is not existed but a page is existed', async () => {
      const pathname = routes.projectSummary.getRedirectPath({
        projectId: 'projectId',
        workspaceName: 'unknown-workspace' as IWorkspace['name'],
      });
      const { component, history } = await makeComponent({ pathname });
      expect(history.location.pathname).toEqual(pathname);
      expect(component.find(NotFoundPage).length).toEqual(1);

      const pathname2 = routes.datasetSummary.getRedirectPath({
        datasetId: 'datasetId',
        workspaceName: 'unknown-workspace' as IWorkspace['name'],
      });
      const case2 = await makeComponent({ pathname: pathname2 });
      expect(case2.history.location.pathname).toEqual(pathname2);
      expect(case2.component.find(NotFoundPage).length).toEqual(1);
    });

    it('should show the 404 page if a workspace is existed but page is not existed', async () => {
      const deleteLastSymbol = (str: string) => str.slice(0, -1);

      const invalidProjectSummaryPathname = deleteLastSymbol(
        routes.projectSummary.getRedirectPath({
          projectId: 'projectId',
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        })
      );
      const case1 = await makeComponent({
        pathname: invalidProjectSummaryPathname,
      });
      expect(case1.history.location.pathname).toEqual(
        invalidProjectSummaryPathname
      );
      expect(case1.component.find(NotFoundPage).length).toEqual(1);

      const invalidDatasetSummaryPathname = deleteLastSymbol(
        routes.datasetSummary.getRedirectPath({
          datasetId: 'datasetId',
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        })
      );
      const case2 = await makeComponent({
        pathname: invalidDatasetSummaryPathname,
      });
      expect(case2.history.location.pathname).toEqual(
        invalidDatasetSummaryPathname
      );
      expect(case2.component.find(NotFoundPage).length).toEqual(1);
    });
  });

  it('should show the 404 page if an unknown page without any workspace', async () => {
    const pathname = '/datasets/unknown-page';
    const { component, history } = await makeComponent({ pathname });

    expect(history.location.pathname).toEqual(pathname);
    expect(component.find(NotFoundPage).length).toEqual(1);
  });
});
