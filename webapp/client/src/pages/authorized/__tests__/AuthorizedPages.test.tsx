import * as React from 'react';

import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { IWorkspace, IUserWorkspaces } from 'core/shared/models/Workspace';
import routes from 'core/shared/routes';
import makeMountComponentWithPredefinedData from 'core/shared/utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'core/shared/utils/tests/mocks/models/workspace';

import { withAct } from 'core/shared/utils/tests/react/helpers';
import DatasetDetailPages from '../DatasetPages/DatasetDetailPages/DatasetDetailPages';
import DatasetsPage from '../DatasetPages/DatasetsPage/DatasetsPage';
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
});
