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
import { Dataset, IDatasetCreationSettings } from 'models/Dataset';
import routes from 'routes';
import DatasetsDataService from 'services/datasets/DatasetsDataService';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import DatasetCreationPage from '../DatasetCreationPage';

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
      Component: DatasetCreationPage,
    });
    await flushAllPromisesFor(data.component);
    return data;
  });
};

const mockDatasetsDataService = makeMockServiceMethod(
  'services/datasets/DatasetsDataService'
);

const nameFieldHelpers = makeAsyncInputHelpersByName('name');
const descriptionFieldHelpers = makeAsyncInputHelpersByName('description');
const tagsFieldHelpers = makeTagsManagerHelpers();

describe('(Pages) DatasetCreationPage', () => {
  it('should disable the "create" button when there are atleast a error', async () => {
    const { component } = await makeComponent();

    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeTruthy();
  });

  it('should display a error only when a field is touched', async () => {
    const { component } = await makeComponent();

    await nameFieldHelpers.change('', nameFieldHelpers.getInput(component));
    expect(findByText('Dataset name is empty!', component).length).toEqual(0);

    await nameFieldHelpers.blur(component);
    expect(findByText('Dataset name is empty!', component).length).toEqual(1);
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

  it('should create a dataset and redirect to the dataset summary page', async () => {
    const { component, history, predefinedData } = await makeComponent();
    const expectedDataset: Dataset = {
      id: 'id',
      tags: ['tag1', 'tag2'],
      name: 'name',
      description: 'description',
    } as any;
    const createDatasetSpy = jest.fn(async () => expectedDataset);
    mockDatasetsDataService.mockServiceMethod(
      DatasetsDataService,
      'createDataset',
      createDatasetSpy
    );

    await nameFieldHelpers.changeAndBlur(expectedDataset.name, component);
    await descriptionFieldHelpers.changeAndBlur(
      expectedDataset.description,
      component
    );
    for (const tag of expectedDataset.tags) {
      await tagsFieldHelpers.addTag(tag, component);
    }
    expect(
      findByDataTestAttribute('create', component).prop('disabled')
    ).toBeFalsy();
    await submitAsyncForm(
      findByDataTestAttribute('create', component),
      component
    );

    const expectedDatasetSettings: IDatasetCreationSettings = {
      name: expectedDataset.name,
      description: expectedDataset.description,
      tags: expectedDataset.tags,
      visibility: 'private',
      type: 'path',
    };
    expect(createDatasetSpy).toBeCalledWith(expectedDatasetSettings);
    expect(history.location.pathname).toBe(
      routes.datasetSummary.getRedirectPath({
        datasetId: expectedDataset.id,
        workspaceName: userWorkspacesWithCurrentUser.user.name,
      })
    );
  });
});
