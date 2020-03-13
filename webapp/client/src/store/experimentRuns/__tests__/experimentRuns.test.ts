import * as R from 'ramda';
import routes from 'routes';

import setupIntegrationTest from 'utils/tests/integrations/setupIntegrationTest';

import { IFilterData, makeDefaultTagFilter } from 'core/features/filter/Model';
import { ISorting } from 'core/shared/models/Sorting';
import checkURLSearchParams from 'core/shared/utils/tests/checkURLSearchParams';
import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import {
  IFilterState,
  selectCurrentContextFilters,
} from 'core/features/filter';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import {
  changePaginationWithLoadingExperimentRuns,
  changeSorting,
  resetExperimentRunsSettings,
  getExperimentRunsOptions,
} from '../actions';
import {
  selectExperimentRunsPagination,
  selectExperimentRunsSorting,
} from '../selectors';
import { loadExperimentRunsActionTypes } from '../types';

const mockProjectId = 'projectId';

const makeMockFilterState = (
  defaultFilters: IFilterData[] = []
): IFilterState => {
  return {
    data: {
      contexts: {
        [mockProjectId]: {
          name: mockProjectId,
          ctx: {
            name: mockProjectId,
            quickFilters: [],
            onApplyFilters: jest.fn(),
          },
          filters: defaultFilters,
        },
      },
      currentContextName: mockProjectId,
    },
  };
};

const runChangePagination = async (currentPage: number) => {
  const integrationTestData = setupIntegrationTest({});
  await integrationTestData.store.dispatch(
    changePaginationWithLoadingExperimentRuns(
      mockProjectId,
      currentPage,
      []
    ) as any
  );
  await flushAllPromises();
  return integrationTestData;
};

const runChangeSorting = async (sorting: ISorting | null) => {
  const integrationTestData = setupIntegrationTest({});
  await integrationTestData.store.dispatch(changeSorting(
    mockProjectId,
    sorting
  ) as any);
  await flushAllPromises();
  return integrationTestData;
};

describe('store', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('experimentRuns', () => {
    describe('pagination', () => {
      it('should correct change current page in store', async () => {
        const { store } = await runChangePagination(2);

        expect(
          selectExperimentRunsPagination(store.getState()).currentPage
        ).toEqual(2);
      });

      it('should load experiment runs with new pagination', async () => {
        const { dispatchSpy } = await runChangePagination(2);
        await flushAllPromises();

        expect(
          R.flatten(dispatchSpy.mock.calls).some(
            ({ type }: any) => type === loadExperimentRunsActionTypes.REQUEST
          )
        ).toBe(true);
      });

      describe('saving pagination', () => {
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

        describe('saving pagination in localStorage', () => {
          it('should save pagination in localStorage', async () => {
            const sorting: ISorting = {
              columnName: 'metrics',
              direction: 'asc',
              fieldName: 'val_acc',
            };
            await runChangeSorting(sorting);

            expect(
              JSON.parse(localStorage[`exprRunsOptions_${mockProjectId}`])
                .sorting
            ).toEqual(sorting);
          });

          it('should delete pagination from localStorage', async () => {
            await runChangeSorting({
              columnName: 'metrics',
              direction: 'asc',
              fieldName: 'val_acc',
            });
            await runChangeSorting(null);

            expect(
              localStorage[`exprRunsOptions_${mockProjectId}`]
            ).toBeFalsy();
          });
        });
      });
    });

    describe('sorting', () => {
      describe('saving sorting', () => {
        describe('saving sorting in URL', () => {
          const changeAndCheckSortingInURL = async (
            sorting: ISorting | null,
            expectedURLParams: {
              sortKey: string | null;
              sortDirection: ISorting['direction'] | null;
            }
          ) => {
            const { history } = await runChangeSorting(sorting);
            checkURLSearchParams(history, expectedURLParams);
          };

          it('should display sortKey param and sortDirection param in URL', async () => {
            await changeAndCheckSortingInURL(
              { columnName: 'metrics', fieldName: 'val_acc', direction: 'asc' },
              { sortKey: 'metrics.val_acc', sortDirection: 'asc' }
            );
          });

          it('should delete sortKey param and sortDirection param in URL if sorting is reseted', async () => {
            await changeAndCheckSortingInURL(null, {
              sortKey: null,
              sortDirection: null,
            });
          });
        });

        describe('saving sorting in localStorage', () => {
          it('should save sorting in localStorage', async () => {
            const sorting: ISorting = {
              columnName: 'metrics',
              direction: 'asc',
              fieldName: 'val_acc',
            };
            await runChangeSorting(sorting);

            expect(
              JSON.parse(localStorage[`exprRunsOptions_${mockProjectId}`])
                .sorting
            ).toEqual(sorting);
          });

          it('should delete sorting from localStorage', async () => {
            await runChangeSorting({
              columnName: 'metrics',
              direction: 'asc',
              fieldName: 'val_acc',
            });
            await runChangeSorting(null);

            expect(
              localStorage[`exprRunsOptions_${mockProjectId}`]
            ).toBeFalsy();
          });
        });
      });
    });

    describe('restore experiment runs pagination and sorting from localStorage', () => {
      const getExperimentRunsPathnameWithSettings = ({
        currentPage,
        sorting,
      }: {
        currentPage: number;
        sorting: ISorting;
      }) => {
        return `${routes.experimentRuns.getRedirectPath({
          workspaceName: userWorkspacesWithCurrentUser.user.name,
          projectId: mockProjectId,
        })}?page=${currentPage + 1}&sortKey=${`${sorting.columnName}.${
          sorting.fieldName
        }`}&sortDirection=${sorting.direction}`;
      };

      it('should apply experiment runs settings from localStorage if they exist', async () => {
        const sorting: ISorting = {
          columnName: 'metrics',
          direction: 'asc',
          fieldName: 'val_acc',
        };
        await runChangeSorting(sorting);
        await runChangePagination(2);

        const { store } = await setupIntegrationTest();
        await store.dispatch(getExperimentRunsOptions(mockProjectId) as any);
        await flushAllPromises();

        expect(
          selectExperimentRunsPagination(store.getState()).currentPage
        ).toEqual(2);
        expect(selectExperimentRunsSorting(store.getState())).toEqual(sorting);
      });

      it('should apply experiment runs settings from URL if they exist', async () => {
        const currentPage = 2;
        const sorting: ISorting = {
          columnName: 'metrics',
          direction: 'asc',
          fieldName: 'val_acc',
        };
        const { store } = setupIntegrationTest({
          pathname: getExperimentRunsPathnameWithSettings({
            currentPage,
            sorting,
          }),
        });

        await store.dispatch(getExperimentRunsOptions(mockProjectId) as any);
        await flushAllPromises();

        expect(
          selectExperimentRunsPagination(store.getState()).currentPage
        ).toEqual(currentPage);
        expect(selectExperimentRunsSorting(store.getState())).toEqual(sorting);
      });

      it('should use settings from URL if settings exist in URL and localStorage', async () => {
        await runChangeSorting({
          columnName: 'metrics',
          direction: 'asc',
          fieldName: 'val_acc',
        });
        await runChangePagination(2);

        const sortingFromURL: ISorting = {
          columnName: 'hyperparameters',
          direction: 'desc',
          fieldName: 'val_acc',
        };
        const currentPageFromURL = 4;
        const { store } = setupIntegrationTest({
          pathname: getExperimentRunsPathnameWithSettings({
            currentPage: currentPageFromURL,
            sorting: sortingFromURL,
          }),
        });

        await store.dispatch(getExperimentRunsOptions(mockProjectId) as any);
        await flushAllPromises();

        expect(
          selectExperimentRunsPagination(store.getState()).currentPage
        ).toEqual(currentPageFromURL);
        expect(selectExperimentRunsSorting(store.getState())).toEqual(
          sortingFromURL
        );
      });
    });

    describe('resetting experiment runs pagination, filters and sorting', () => {
      const runResetExperimentRunsOptions = async () => {
        const integrationTestData = await setupIntegrationTest({
          initialState: {
            filters: makeMockFilterState([makeDefaultTagFilter('demo')]),
          },
        });
        await integrationTestData.store.dispatch(changeSorting(mockProjectId, {
          columnName: 'metrics',
          fieldName: 'val_acc',
          direction: 'asc',
        }) as any);
        await integrationTestData.store.dispatch(
          changePaginationWithLoadingExperimentRuns(mockProjectId, 5, []) as any
        );
        await flushAllPromises();

        await integrationTestData.store.dispatch(resetExperimentRunsSettings(
          mockProjectId
        ) as any);
        await flushAllPromises();

        return integrationTestData;
      };

      it('should reset experiment runs options in store', async () => {
        const { store } = await runResetExperimentRunsOptions();

        expect(
          selectExperimentRunsPagination(store.getState()).currentPage
        ).toBe(0);
        expect(selectExperimentRunsSorting(store.getState())).toBe(null);
        expect(selectCurrentContextFilters(store.getState())).toEqual([]);
      });

      it('should reset experiment runs options in URL', async () => {
        const { history } = await runResetExperimentRunsOptions();

        checkURLSearchParams(history, {
          sortKey: null,
          sortDirection: null,
          filters: null,
          page: null,
        });
      });

      it('should reset experiment runs options in localStorage', async () => {
        const {} = await runResetExperimentRunsOptions();

        expect(localStorage[`exprRunsOptions_${mockProjectId}`]).toBeFalsy();
      });
    });
  });
});

export {};
