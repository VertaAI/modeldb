import {
  fireEvent,
  waitForElementToBeRemoved,
  waitForElement,
} from '@testing-library/dom';
import { cleanup } from '@testing-library/react';
import * as React from 'react';
import { Route, Switch } from 'react-router-dom';
import { Store } from 'redux';
import routes from 'routes';

import { HttpError } from 'core/shared/models/Error';
import delay from 'core/shared/utils/delay';
import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import { Project } from 'models/Project';
import ProjectsPage from 'pages/authorized/ProjectsPages/ProjectsPage/ProjectsPage';
import { ProjectDataService } from 'services/projects';
import { makeComponentForIntegratingTest } from 'utils/tests/integrations/makeMountComponentForIntegratingTest';
import { makeProject } from 'utils/tests/mocks/models/projectsMocks';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import ProjectDetailsPage from '../../ProjectDetailsPage';

// need for testing portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element: any) => {
      return element;
    },
  };
});

const makePresetProject = ({  }: {}) => {
  return makeProject({
    id: 'project-id',
    description: 'description',
    name: 'project',
  });
};

const makePresetProjectForCurrentUser = ({  }: {}) => {
  return makePresetProject({});
};

jest.mock('services/projects/ProjectDataService');
const setProjectInStore = async (store: Store, project: Project) => {
  (ProjectDataService as any).mockImplementation(() => {
    const mock: Partial<ProjectDataService> = {
      loadProject: async (id: string) => {
        if (id === project.id) {
          return project;
        }
        throw new HttpError({
          status: 404,
          type: 'entityNotFound',
        });
      },
      deleteProject: async () => {
        await delay(1);
      },
      loadProjects: async () => ({ totalCount: 1, data: [project] }),
    };
    return mock;
  });
};

const makeComponent = async ({
  project,
  viewedDatasetId = project.id,
}: {
  project: Project;
  viewedDatasetId?: string;
}) => {
  const integrationTestData = await makeComponentForIntegratingTest(
    () => (
      <Switch>
        <Route
          path={routes.projectSummary.getPath()}
          exact={true}
          component={ProjectDetailsPage}
        />
        <Route
          path={routes.projects.getPath()}
          exact={true}
          component={ProjectsPage}
        />
      </Switch>
    ),
    {
      pathname: routes.projectSummary.getRedirectPath({
        workspaceName: userWorkspacesWithCurrentUser.user.name,
        projectId: viewedDatasetId,
      }),
    },
    async store => {
      await setProjectInStore(store, project);
    }
  );

  await flushAllPromises();

  return { ...integrationTestData, data: { project } };
};

describe('(pages) ProjectSummaryPage', () => {
  afterEach(cleanup);

  it('should render info about a project when it is loaded', async () => {
    const {
      component: { queryByTestId },
      data,
    } = await makeComponent({
      project: makePresetProjectForCurrentUser({}),
    });

    expect(queryByTestId('summary-info-id')!.textContent).toBe(data.project.id);
  });

  describe('deleting', () => {
    it('should display the delete button', async () => {
      const {
        component: { queryByTestId },
      } = await makeComponent({
        project: makePresetProjectForCurrentUser({}),
      });

      expect(queryByTestId('delete-project-button')).not.toBeNull();
    });

    it('should delete a project and redirect to the projects page after a project deleting', async () => {
      const {
        component: { queryByTestId },
        history,
      } = await makeComponent({
        project: makePresetProject({}),
      });

      fireEvent.click(queryByTestId('delete-project-button')!);
      fireEvent.click(queryByTestId('confirm-ok-button')!);

      await waitForElementToBeRemoved(() => queryByTestId('summary-info-id'));

      expect(history.location.pathname).toEqual(
        routes.projects.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        })
      );
    });
  });

  describe('when there are some loading error', () => {
    it('should display a error', async () => {
      const {
        component: { queryByTestId },
      } = await makeComponent({
        project: makePresetProjectForCurrentUser({}),
        viewedDatasetId: 'notExistedDatasetId',
      });

      expect(queryByTestId('page-error')).not.toBeNull();
    });

    it('should not display a project error when some a project have error and user select another project without a error', async () => {
      const project = makePresetProjectForCurrentUser({});
      const {
        component: { queryByTestId },
        history,
      } = await makeComponent({
        project,
        viewedDatasetId: 'notExistedDataset',
      });

      history.push(
        routes.projects.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.id,
        })
      );
      await waitForElement(() => queryByTestId('projects'));
      history.push(
        routes.projectSummary.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.id,
          projectId: project.id,
        })
      );

      await waitForElement(() => queryByTestId('summary-info-id'));
    });
  });
});
