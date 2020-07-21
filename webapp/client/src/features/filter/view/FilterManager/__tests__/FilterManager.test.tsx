import * as React from 'react';
import { ReactWrapper } from 'enzyme';
import { Popper } from '@material-ui/core';

import {
  defaultQuickFilters,
  IFilterData,
  makeURLFilters,
  makeDefaultStringFilter,
} from 'shared/models/Filters';
import makeMountComponentWithPredefinedData from 'shared/utils/tests/makeMountComponentWithPredefinedData';
import routes from 'shared/routes';
import { userWorkspacesWithCurrentUser } from 'shared/utils/tests/mocks/models/workspace';
import waitFor from 'shared/utils/tests/integrations/waitFor';
import {
  findByDataTestAttribute,
  makeInputHelpers,
} from 'shared/utils/tests/react/helpers';

import FilterManager from '../FilterManager';
import { makeFilterSelectHelpers } from '../InstantFilterItem/FilterSelect/__tests__/helpers';

const quickFiltersMap = {
  name: defaultQuickFilters.name,
  tag: defaultQuickFilters.tag,
  owner: defaultQuickFilters.owner,
};

const makeComponent = async ({
  predefinedFilters,
}: {
  predefinedFilters?: IFilterData[];
}) => {
  const onAppyFiltersSpy = jest.fn();
  const props: React.ComponentProps<typeof FilterManager> = {
    isCollapsed: false,
    onExpandSidebar: jest.fn(),
    context: {
      name: 'test',
      quickFilters: Object.values(quickFiltersMap),
      onApplyFilters: onAppyFiltersSpy,
    },
    title: 'title',
  };
  const data = await makeMountComponentWithPredefinedData({
    Component: () => <FilterManager {...props} />,
    settings: {
      pathname: (() => {
        if (predefinedFilters) {
          return routes.experimentRuns.getRedirectPathWithQueryParams({
            params: {
              projectId: 'projectId',
              workspaceName: userWorkspacesWithCurrentUser.user.name,
            },
            queryParams: {
              filters: makeURLFilters(predefinedFilters),
            },
          });
        }
      })(),
    },
  });
  await waitFor(data.component);

  onAppyFiltersSpy.mockClear();
  return { ...data, spies: { onAppyFilters: onAppyFiltersSpy } };
};

const makeFilterItemHelpers = (getFilterItem: () => ReactWrapper) => {
  const findPropertySelect = () => {
    return makeFilterSelectHelpers(() =>
      findByDataTestAttribute('filter-select', getFilterItem()).first()
    );
  };

  const findOperatorSelect = () => {
    return makeFilterSelectHelpers(() =>
      findByDataTestAttribute('filter-select', getFilterItem()).at(1)
    );
  };

  const isOpened = () => {
    return (
      getFilterItem()
        .children()
        .find(Popper).length === 1 &&
      findByDataTestAttribute('remove-filter', getFilterItem()).length === 1
    );
  };

  const isActive = () => {
    return findByDataTestAttribute(
      'active-filter-checkbox',
      getFilterItem()
    ).prop('checked');
  };

  const remove = async (component: ReactWrapper) => {
    if (!isOpened()) {
      await open();
    }
    findByDataTestAttribute('remove-filter', getFilterItem()).simulate('click');
    await waitFor(component);
  };

  const findFilterValueField = makeInputHelpers(() =>
    findByDataTestAttribute('filter-item-value', getFilterItem())
  );

  const getDisabledInputs = () => {
    return {
      valueField: findFilterValueField.isDisabled(getFilterItem()),
      operatorSelect: findOperatorSelect().isDisabled(),
      propertyNameSelect: findPropertySelect().isDisabled(),
    };
  };

  const getInputsValues = () => ({
    valueField: findFilterValueField.getValue(getFilterItem()),
    operatorSelect: findOperatorSelect().getDisplayedSelectedOption(),
    propertyNameSelect: findPropertySelect().getDisplayedSelectedOption(),
  });

  const open = async () => {
    jest.useFakeTimers();
    const filterItem = getFilterItem();
    findByDataTestAttribute('open-filter', filterItem).simulate('click');
    filterItem.update();
    await waitFor(filterItem);
    jest.runAllTimers();
    await waitFor(filterItem);
    jest.useRealTimers();
  };

  return {
    isOpened,
    isActive,
    findPropertySelect,
    findOperatorSelect,
    findFilterValueField,
    open,
    getDisabledInputs,
    getInputsValues,
    remove,
  };
};

const expectOnAppyFiltersToBeCalledWithFilters = (
  fn: jest.Mock<any>,
  filters: IFilterData[]
) => {
  expect(fn).toBeCalled();
  expect(fn.mock.calls[0][0]).toEqual(filters);
};

const renderComponentAndDeleteFilter = async ({
  filter,
}: {
  filter: IFilterData;
}) => {
  const { component, spies } = await makeComponent({
    predefinedFilters: [filter],
  });
  const newFilterItemHelpers = makeFilterItemHelpers(() =>
    findByDataTestAttribute('filter-item', component)
  );
  await newFilterItemHelpers.remove(component);

  expect(findByDataTestAttribute('filter-item', component).length).toEqual(0);

  return { spies };
};

describe('features (filter)', () => {
  describe('FilterManager', () => {
    it('should create an empty, active and opened filter by name and don`t apply it after click on the "add filter" button', async () => {
      const { component, spies } = await makeComponent({});

      spies.onAppyFilters.mockClear();
      findByDataTestAttribute('add-filter', component).simulate('click');
      await waitFor(component);

      const newFilterItem = findByDataTestAttribute('filter-item', component);
      const newFilterItemHelpers = makeFilterItemHelpers(() =>
        findByDataTestAttribute('filter-item', component)
      );

      expect(
        newFilterItemHelpers.findPropertySelect().getDisplayedSelectedOption()
      ).toEqual('name');
      expect(
        newFilterItemHelpers.findOperatorSelect().getDisplayedSelectedOption()
      ).toEqual('=');
      expect(
        newFilterItemHelpers.findFilterValueField.getValue(newFilterItem)
      ).toEqual('');

      expect(newFilterItemHelpers.isActive()).toEqual(true);
      expect(newFilterItemHelpers.isOpened()).toBeTruthy();

      expect(spies.onAppyFilters).not.toBeCalled();
    });

    describe('filter editing', () => {
      it('should open and make editable a filter after click on a filter', async () => {
        const filter = makeDefaultStringFilter(
          quickFiltersMap.name.propertyName,
          'name',
          'EQUALS'
        );
        const { component, spies } = await makeComponent({
          predefinedFilters: [filter],
        });
        const newFilterItemHelpers = makeFilterItemHelpers(() =>
          findByDataTestAttribute('filter-item', component)
        );

        expect(newFilterItemHelpers.isOpened()).toBeFalsy();
        expect(newFilterItemHelpers.getDisabledInputs()).toEqual({
          operatorSelect: true,
          propertyNameSelect: true,
          valueField: true,
        } as ReturnType<typeof newFilterItemHelpers.getDisabledInputs>);

        await newFilterItemHelpers.open();

        expect(spies.onAppyFilters).not.toBeCalled();
        expect(newFilterItemHelpers.isOpened()).toBeTruthy();
        expect(newFilterItemHelpers.getDisabledInputs()).toEqual({
          operatorSelect: false,
          propertyNameSelect: false,
          valueField: false,
        } as ReturnType<typeof newFilterItemHelpers.getDisabledInputs>);
      });

      describe('when filter is active', () => {
        describe('when value is empty', () => {
          it('should change a property name without filters applying', async () => {
            const filter = makeDefaultStringFilter(
              quickFiltersMap.name.propertyName,
              '',
              'EQUALS'
            );
            const newPropertyName = quickFiltersMap.tag.propertyName;

            const { component, spies } = await makeComponent({
              predefinedFilters: [filter],
            });
            const newFilterItemHelpers = makeFilterItemHelpers(() =>
              findByDataTestAttribute('filter-item', component)
            );

            await newFilterItemHelpers.open();
            await newFilterItemHelpers
              .findPropertySelect()
              .changeOption(newPropertyName);

            expect(spies.onAppyFilters).not.toBeCalled();
            expect(newFilterItemHelpers.isOpened()).toBeTruthy();
            expect(newFilterItemHelpers.getInputsValues()).toEqual({
              operatorSelect: '=',
              propertyNameSelect: newPropertyName,
              valueField: '',
            });
          });

          it('should change an operator without filters applying', async () => {
            const filter = makeDefaultStringFilter(
              quickFiltersMap.name.propertyName,
              '',
              'EQUALS'
            );

            const { component, spies } = await makeComponent({
              predefinedFilters: [filter],
            });
            const newFilterItemHelpers = makeFilterItemHelpers(() =>
              findByDataTestAttribute('filter-item', component)
            );

            await newFilterItemHelpers.open();
            await newFilterItemHelpers.findOperatorSelect().changeOption('!=');

            expect(spies.onAppyFilters).not.toBeCalled();
            expect(newFilterItemHelpers.isOpened()).toBeTruthy();
            expect(newFilterItemHelpers.getInputsValues()).toEqual({
              operatorSelect: '!=',
              propertyNameSelect: filter.name,
              valueField: '',
            });
          });

          it('should delete a filter without filters applying', async () => {
            const { spies } = await renderComponentAndDeleteFilter({
              filter: makeDefaultStringFilter(
                quickFiltersMap.name.propertyName,
                '',
                'EQUALS'
              ),
            });

            expect(spies.onAppyFilters).not.toBeCalled();
          });
        });

        it('should change a value and should apply filters after input blur if a value is changed', async () => {
          const filter = makeDefaultStringFilter(
            quickFiltersMap.name.propertyName,
            '',
            'EQUALS'
          );
          const filterValue = 'value';

          const { component, spies } = await makeComponent({
            predefinedFilters: [filter],
          });
          const newFilterItemHelpers = makeFilterItemHelpers(() =>
            findByDataTestAttribute('filter-item', component)
          );

          await newFilterItemHelpers.open();
          await newFilterItemHelpers.findFilterValueField.changeAndBlur(
            filterValue,
            component
          );

          expectOnAppyFiltersToBeCalledWithFilters(spies.onAppyFilters, [
            { ...filter, value: filterValue },
          ]);
          expect(newFilterItemHelpers.isOpened()).toBeTruthy();
          expect(newFilterItemHelpers.getInputsValues()).toEqual({
            operatorSelect: '=',
            propertyNameSelect: filter.name,
            valueField: 'value',
          });
        });

        it('should not change a value and should not apply filters after input blur if a value is not changed', async () => {
          const filter = makeDefaultStringFilter(
            quickFiltersMap.name.propertyName,
            '',
            'EQUALS'
          );

          const { component, spies } = await makeComponent({
            predefinedFilters: [filter],
          });
          const newFilterItemHelpers = makeFilterItemHelpers(() =>
            findByDataTestAttribute('filter-item', component)
          );

          await newFilterItemHelpers.open();
          newFilterItemHelpers.findFilterValueField.blur(component);

          expect(spies.onAppyFilters).not.toBeCalled();
          expect(newFilterItemHelpers.isOpened()).toBeTruthy();
          expect(newFilterItemHelpers.getInputsValues()).toEqual({
            operatorSelect: '=',
            propertyNameSelect: filter.name,
            valueField: '',
          });
        });

        it('should delete a filter and apply filters', async () => {
          const { spies } = await renderComponentAndDeleteFilter({
            filter: makeDefaultStringFilter(
              quickFiltersMap.name.propertyName,
              'value',
              'EQUALS'
            ),
          });

          expectOnAppyFiltersToBeCalledWithFilters(spies.onAppyFilters, []);
        });
      });

      describe('when filter is not active', () => {
        it('should delete a filter without filters applying', async () => {
          const { spies } = await renderComponentAndDeleteFilter({
            filter: {
              ...makeDefaultStringFilter(
                quickFiltersMap.name.propertyName,
                'value',
                'EQUALS'
              ),
              isActive: false,
            },
          });

          expect(spies.onAppyFilters).not.toBeCalled();
        });

        it('should not apply filters on any changes', async () => {
          const filter = {
            ...makeDefaultStringFilter(
              quickFiltersMap.name.propertyName,
              'value',
              'EQUALS'
            ),
            isActive: false,
          };
          const newPropertyName = quickFiltersMap.tag.propertyName;

          const { component, spies } = await makeComponent({
            predefinedFilters: [filter],
          });

          const newFilterItemHelpers = makeFilterItemHelpers(() =>
            findByDataTestAttribute('filter-item', component)
          );
          await newFilterItemHelpers.open();

          await newFilterItemHelpers
            .findPropertySelect()
            .changeOption(newPropertyName);
          expect(spies.onAppyFilters).not.toBeCalled();

          await newFilterItemHelpers.findOperatorSelect().changeOption('!=');
          expect(spies.onAppyFilters).not.toBeCalled();

          await newFilterItemHelpers.findFilterValueField.changeAndBlur(
            'new-value',
            component
          );
          expect(spies.onAppyFilters).not.toBeCalled();

          expect(newFilterItemHelpers.getInputsValues()).toEqual({
            propertyNameSelect: newPropertyName,
            operatorSelect: '!=',
            valueField: 'new-value',
          });
        });
      });
    });
  });
});
