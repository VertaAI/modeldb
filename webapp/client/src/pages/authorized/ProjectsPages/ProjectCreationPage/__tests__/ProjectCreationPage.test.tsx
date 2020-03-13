import * as React from 'react';
import { act } from 'react-dom/test-utils';

import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { makeMockServiceMethod } from 'core/shared/utils/tests/integrations/mockServiceMethod';
import {
  makeAsyncInputHelpersByName,
  findByDataTestAttribute,
  findByText,
  withAct,
  submitAsyncForm,
} from 'core/shared/utils/tests/react/helpers';
import { makeTagsManagerHelpers } from 'core/shared/view/domain/TagsManager/__tests__/helpers';
import { Project, IProjectCreationSettings } from 'models/Project';
import routes from 'routes';
import { ProjectDataService } from 'services/projects';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { makeProject } from 'utils/tests/mocks/models/projectsMocks';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import ProjectCreationPage from '../ProjectCreationPage';

// need for testing portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element: any) => {
      return element;
    },
  };
});

const makeComponent = async () => {
  return await withAct(async () => {
    const data = await makeMountComponentWithPredefinedData({
      Component: ProjectCreationPage,
    });
    data.component.update();
    return data;
  });
};

const mockProjectsDataService = makeMockServiceMethod(
  'services/projects/ProjectDataService'
);

const nameFieldHelpers = makeAsyncInputHelpersByName('name');
const descriptionFieldHelpers = makeAsyncInputHelpersByName('description');
const tagsFieldHelpers = makeTagsManagerHelpers();

describe('(Pages) ProjectCreationPage', () => {
  it('should disable the "create" button when there are atleast a error', async () => {
    const { component } = await makeComponent();

    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeTruthy();
  });

  it('should display a error only when a field is touched', async () => {
    const { component } = await makeComponent();

    await nameFieldHelpers.change('', nameFieldHelpers.getInput(component));
    expect(findByText('Project name is empty!', component).length).toEqual(0);

    await nameFieldHelpers.blur(component);
    expect(findByText('Project name is empty!', component).length).toEqual(1);
  });

  it('should display all errors when some field is not touched and a user try to create a project', async () => {
    const { component } = await makeComponent();

    await nameFieldHelpers.changeAndBlur('name', component);
    await descriptionFieldHelpers.change(
      Array.from({ length: 300 }, (_, i) => i).join(''),
      component
    );
    await act(async () => {
      findByDataTestAttribute('create', component).simulate('click');
    });

    expect(findByDataTestAttribute('field-error').length).not.toEqual(0);
    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeTruthy();
  });

  it('should create a project and redirect to the project summary page', async () => {
    const { component, history } = await makeComponent();
    const expectedProject: Project = makeProject({
      id: 'id',
      tags: ['tag1', 'tag2'],
      name: 'name',
      description: 'description',
    });
    const createProjectSpy = jest.fn(async () => expectedProject);
    mockProjectsDataService.mockServiceMethod(
      ProjectDataService,
      'createProject',
      createProjectSpy
    );

    await nameFieldHelpers.changeAndBlur(expectedProject.name, component);
    await descriptionFieldHelpers.changeAndBlur(
      expectedProject.description,
      component
    );
    for (const tag of expectedProject.tags) {
      await tagsFieldHelpers.addTag(tag, component);
    }
    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeFalsy();
    await submitAsyncForm(
      findByDataTestAttribute('create', component),
      component
    );
    await flushAllPromisesFor(component);

    const expectedProjectSettings: IProjectCreationSettings = {
      name: expectedProject.name,
      description: expectedProject.description,
      tags: expectedProject.tags,
      visibility: 'private',
    };
    expect(createProjectSpy).toBeCalledWith(expectedProjectSettings);
    expect(history.location.pathname).toBe(
      routes.projectSummary.getRedirectPath({
        projectId: expectedProject.id,
        workspaceName: userWorkspacesWithCurrentUser.user.name,
      })
    );
  });
});
