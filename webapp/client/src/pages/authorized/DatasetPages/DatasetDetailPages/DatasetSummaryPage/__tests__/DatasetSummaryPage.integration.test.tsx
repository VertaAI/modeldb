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
import { IDataset, Dataset } from 'models/Dataset';
import { DatasetsDataService } from 'services/datasets';
import { makeComponentForIntegratingTest } from 'utils/tests/integrations/makeMountComponentForIntegratingTest';
import { makeDataset } from 'utils/tests/mocks/models/datasetMocks';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import DatasetsPage from '../../../DatasetsPage';
import DatasetDetailPages from '../../DatasetDetailPages';

// need for testing portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element: any) => {
      return element;
    },
  };
});

const makePresetDataset = ({  }: {}) =>
  makeDataset({
    id: 'dataset-id',
    attributes: [],
    description: 'description',
    name: 'dataset',
    type: 'path',
  });

const makePresetDatasetForCurrentUser = ({  }: {}) => {
  return makePresetDataset({});
};

jest.mock('services/datasets/DatasetsDataService');
const setDatasetInStore = async (store: Store, dataset: IDataset) => {
  (DatasetsDataService as any).mockImplementation(() => {
    const mock: Partial<DatasetsDataService> = {
      loadDataset: async (id: string) => {
        if (id === dataset.id) {
          return dataset;
        }
        throw new HttpError({
          status: 404,
          type: 'entityNotFound',
        });
      },
      deleteDataset: async () => {
        await delay(1);
      },
      loadDatasets: async () => ({ totalCount: 1, data: [dataset] }),
    };
    return mock;
  });
};

const makeComponent = async ({
  dataset,
  viewedDatasetId = dataset.id,
}: {
  dataset: IDataset;
  viewedDatasetId?: string;
}) => {
  const integrationTestData = await makeComponentForIntegratingTest(
    () => (
      <Switch>
        <Route
          path={routes.datasetSummary.getPath()}
          exact={true}
          component={DatasetDetailPages}
        />
        <Route
          path={routes.datasets.getPath()}
          exact={true}
          component={DatasetsPage}
        />
      </Switch>
    ),
    {
      pathname: routes.datasetSummary.getRedirectPath({
        datasetId: viewedDatasetId,
        workspaceName: userWorkspacesWithCurrentUser.user.name,
      }),
    },
    async store => {
      await setDatasetInStore(store, dataset);
    }
  );

  await flushAllPromises();

  return { ...integrationTestData, data: { dataset } };
};

describe('(pages) DatasetSummaryPage', () => {
  afterEach(cleanup);

  it('should render info about a dataset when it is loaded', async () => {
    const {
      component: { queryByTestId },
      data,
    } = await makeComponent({
      dataset: makePresetDatasetForCurrentUser({}),
    });

    expect(queryByTestId('summary-info-id')!.textContent).toBe(data.dataset.id);
  });

  describe('deleting', () => {
    it('should display the delete button', async () => {
      const {
        component: { queryByTestId },
      } = await makeComponent({
        dataset: makePresetDatasetForCurrentUser({}),
      });

      expect(queryByTestId('delete-dataset-button')).not.toBeNull();
    });

    it('should delete a dataset and redirect to the datasets page after a dataset deleting', async () => {
      const {
        component: { queryByTestId },
        history,
      } = await makeComponent({
        dataset: makePresetDatasetForCurrentUser({}),
      });

      fireEvent.click(queryByTestId('delete-dataset-button')!);
      fireEvent.click(queryByTestId('confirm-ok-button')!);

      await waitForElementToBeRemoved(() => queryByTestId('summary-info-id'));

      expect(history.location.pathname).toEqual(
        routes.datasets.getRedirectPath({
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
        dataset: makePresetDatasetForCurrentUser({}),
        viewedDatasetId: 'notExistedDatasetId',
      });

      expect(queryByTestId('page-error')).not.toBeNull();
    });

    it('should not display a dataset error when some a dataset have error and user select another dataset without a error', async () => {
      const dataset = makePresetDatasetForCurrentUser({});
      const {
        component: { queryByTestId },
        history,
      } = await makeComponent({
        dataset,
        viewedDatasetId: 'notExistedDataset',
      });

      history.push(
        routes.datasets.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        })
      );
      await waitForElement(() => queryByTestId('datasets'));
      history.push(
        routes.datasetSummary.getRedirectPath({
          datasetId: dataset.id,
          workspaceName: userWorkspacesWithCurrentUser.user.name,
        })
      );

      await waitForElement(() => queryByTestId('summary-info-id'));
    });
  });
});
