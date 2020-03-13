import * as R from 'ramda';

import checkURLSearchParams from 'core/shared/utils/tests/checkURLSearchParams';
import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import routes from 'routes';
import setupIntegrationTest from 'utils/tests/integrations/setupIntegrationTest';

import {
  changeExperimentsPaginationWithLoading,
  getDefaultExperimentsOptions,
} from '../actions';
import { selectExperimentsPagination } from '../selectors';
import { loadExperimentsActionTypes } from '../types';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

const mockProjectId = 'project-id';

describe('store', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('experiments', () => {
    describe('pagination', () => {
      const runChangePagination = async (currentPage: number) => {
        const integrationTestData = setupIntegrationTest({});
        integrationTestData.store.dispatch(
          changeExperimentsPaginationWithLoading(
            mockProjectId,
            currentPage,
            []
          ) as any
        );
        await flushAllPromises();
        return integrationTestData;
      };

      it('should correct change current page in store', async () => {
        const { store } = await runChangePagination(2);

        expect(
          selectExperimentsPagination(store.getState()).currentPage
        ).toEqual(2);
      });

      it('should load experiments with new pagination', async () => {
        const { dispatchSpy } = await runChangePagination(2);
        await flushAllPromises();

        expect(
          R.flatten(dispatchSpy.mock.calls).some(
            ({ type }: any) => type === loadExperimentsActionTypes.REQUEST
          )
        ).toBe(true);
      });

      describe('saving pagination in URL', () => {
        const changeAndCheckCurrentPageInURL = async (
          newCurrentPage: number,
          expectedCurrentPageInURL: string | null
        ) => {
          const { history } = await runChangePagination(newCurrentPage);
          checkURLSearchParams(history, { page: expectedCurrentPageInURL });
        };

        it('should display incremented current page', async () => {
          await changeAndCheckCurrentPageInURL(2, '3');
          await changeAndCheckCurrentPageInURL(3, '4');
        });

        it('should delete current page if current page is the first page', async () => {
          await changeAndCheckCurrentPageInURL(0, null);
        });
      });

      describe('restoring pagination from URL', async () => {
        const { store } = setupIntegrationTest({
          pathname: `${routes.projects.getRedirectPath({
            workspaceName: userWorkspacesWithCurrentUser.user.name,
          })}?page=${3}`,
        });

        await store.dispatch(getDefaultExperimentsOptions(
          mockProjectId
        ) as any);

        expect(
          selectExperimentsPagination(store.getState()).currentPage
        ).toEqual(2);
      });
    });
  });
});
