import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { Route } from 'react-router';

import { flushAllPromisesFor } from 'shared/utils/tests/integrations/flushAllPromisesFor';
import { makeMockServiceMethod } from 'shared/utils/tests/integrations/mockServiceMethod';
import {
  makeAsyncInputHelpersByName,
  findByDataTestAttribute,
  findByText,
  withAct,
  submitAsyncForm,
} from 'shared/utils/tests/react/helpers';
import { makeTagsManagerHelpers } from 'shared/view/domain/BaseTagsManager/__tests__/helpers';
import Experiment, {
  IExperimentCreationSettings,
} from 'shared/models/Experiment';
import routes from 'shared/routes';
import ExperimentsDataService from 'services/experiments/ExperimentsDataService';
import makeMountComponentWithPredefinedData from 'shared/utils/tests/integrations/makeMountComponentWithPredefinedData';

import { userWorkspacesWithCurrentUser } from 'shared/utils/tests/mocks/models/workspace';
import ExperimentCreation from '../ExperimentCreation';

// need for testing portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element: any) => {
      return element;
    },
  };
});

const projectId = 'project-id';

const makeComponent = async () => {
  return await withAct(async () => {
    const data = await makeMountComponentWithPredefinedData({
      Component: () => <ExperimentCreation projectId={projectId} />,
      settings: {
        pathname: routes.experimentCreation.getRedirectPath({
          projectId,
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        }),
      },
    });
    await flushAllPromisesFor(data.component);
    await flushAllPromisesFor(data.component);
    return data;
  });
};
const mockExperimentsDataService = makeMockServiceMethod(
  'services/experiments/ExperimentsDataService'
);

const nameFieldHelpers = makeAsyncInputHelpersByName('name');
const descriptionFieldHelpers = makeAsyncInputHelpersByName('description');
const tagsFieldHelpers = makeTagsManagerHelpers();

describe('(feature) ExperimentCreation', () => {
  it('should disable the "create" button when there are atleast a error', async () => {
    const { component } = await makeComponent();

    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeTruthy();
  });

  it('should display a error only when a field is touched', async () => {
    const { component } = await makeComponent();

    await nameFieldHelpers.change('', nameFieldHelpers.getInput(component));
    expect(findByText('Experiment name is empty!', component).length).toEqual(
      0
    );

    await nameFieldHelpers.blur(component);
    expect(findByText('Experiment name is empty!', component).length).toEqual(
      1
    );
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

  it('should create a experiment and redirect to the experiments page', async () => {
    const expectedExperiment: Experiment = {
      id: 'id',
      projectId,
      tags: ['tag1', 'tag2'],
      name: 'name',
      description: 'description',
    } as any;
    const createExperimentSpy = jest.fn(async () => expectedExperiment);
    mockExperimentsDataService.mockServiceMethod(
      ExperimentsDataService,
      'createExperiment',
      createExperimentSpy
    );
    const { component, history } = await makeComponent();

    await nameFieldHelpers.changeAndBlur(expectedExperiment.name, component);
    await descriptionFieldHelpers.changeAndBlur(
      expectedExperiment.description,
      component
    );
    for (const tag of expectedExperiment.tags) {
      await tagsFieldHelpers.addTag(tag, component);
    }
    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeFalsy();
    await submitAsyncForm(
      findByDataTestAttribute('create', component),
      component
    );

    const expectedExperimentSettings: IExperimentCreationSettings = {
      name: expectedExperiment.name,
      description: expectedExperiment.description,
      tags: expectedExperiment.tags,
    };
    expect(createExperimentSpy).toBeCalledWith(
      projectId,
      expectedExperimentSettings
    );
    expect(history.location.pathname).toBe(
      routes.experiments.getRedirectPath({
        projectId: expectedExperiment.projectId,
        workspaceName: userWorkspacesWithCurrentUser.user.name,
      })
    );
  });
});
