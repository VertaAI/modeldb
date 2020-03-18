import { mount, ReactWrapper } from 'enzyme';
import * as React from 'react';

import {
  IQuickFilter,
  defaultQuickFilters,
  IFilterData,
} from 'core/features/filter/Model';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';

import QuickFilterInput, {
  IQuickFilterInputLocalProps,
} from '../QuickFilterInput';

const mockQuickFilters: IQuickFilter[] = [
  defaultQuickFilters.name,
  defaultQuickFilters.tag,
  defaultQuickFilters.description,
];
const renderComponent = (props: Partial<IQuickFilterInputLocalProps> = {}) => {
  const defaultProps: IQuickFilterInputLocalProps = {
    isCollapsed: false,
    onCreateFilter: jest.fn(),
    quickFilters: mockQuickFilters,
    onExpandSidebar: jest.fn(),
  };
  return mount(<QuickFilterInput {...defaultProps} {...props} />);
};

const renderComponentWithSelectedQuickFilter = (
  caption: string,
  props: Partial<IQuickFilterInputLocalProps> = {}
) => {
  const defaultProps: IQuickFilterInputLocalProps = {
    isCollapsed: false,
    onCreateFilter: jest.fn(),
    quickFilters: mockQuickFilters,
    onExpandSidebar: jest.fn(),
  };
  const component = mount(<QuickFilterInput {...defaultProps} {...props} />);

  addQuickFilter(caption, component);

  return component;
};

const findInput = (component: ReactWrapper<any, any>) => {
  return findByDataTestAttribute('quick-filter-input', component);
};

const findQuickFilterItems = (component: ReactWrapper<any, any>) => {
  return component.find(`[data-test^="quick-filter-item"]`);
};

const createFilterFromSelectedQuickFilter = (
  component: ReactWrapper<any, any>
) => {
  component.find('input').simulate('keyup', { key: 'Enter' });
};

const addQuickFilter = (
  caption: string,
  component: ReactWrapper<any, Readonly<{}>, React.Component>
) => {
  findInput(component).simulate('click');
  const quickFilterItemsElems = findQuickFilterItems(component);
  const targetQuickFilterItemElem: ReactWrapper<any, any> = (() => {
    for (let i = 0; i < quickFilterItemsElems.length; i += 1) {
      if (
        quickFilterItemsElems
          .at(i)
          .text()
          .includes(caption)
      ) {
        return quickFilterItemsElems.at(i);
      }
    }
  })()!;
  targetQuickFilterItemElem.simulate('click');
};

const checkInputIsFocused = (component: ReactWrapper) => {
  return findInput(component).getDOMNode() === document.activeElement;
};

// todo update tests
describe('component', () => {
  describe('QuickFilterInput', () => {
    describe('when a quick filter is not collapsed', () => {
      describe('if a quick filter is not selected', () => {
        it('should render available quick filters on click by input', () => {
          const component = renderComponent();

          findInput(component).simulate('click');

          const quickFilterItemsElems = findQuickFilterItems(component);
          expect(quickFilterItemsElems.length).toBe(mockQuickFilters.length);
          mockQuickFilters.forEach((mockQuickFilter, i) => {
            expect(quickFilterItemsElems.at(i).text()).toEqual(
              mockQuickFilter.caption
            );
          });
        });

        it('should not handle an input', () => {
          const component = renderComponent();
          const inputElem = findInput(component);

          inputElem.simulate('change', {
            target: { value: 'project' },
          });

          expect(inputElem.prop('value')).toBe('');
        });
      });

      describe('if a quick filter is selected', () => {
        it('should focus on input after select a quick filter', () => {
          const component = renderComponent();

          addQuickFilter(mockQuickFilters[0].caption!, component);

          expect(checkInputIsFocused(component)).toEqual(true);
        });

        it('should render selected quick filter as removable tag', () => {
          const selectedFilter = mockQuickFilters[0];
          const component = renderComponentWithSelectedQuickFilter(
            mockQuickFilters[0].caption!
          );

          const selectedQuickFilterElem = findByDataTestAttribute(
            'selected-quick-filter',
            component
          );
          expect(
            selectedQuickFilterElem.text().includes(selectedFilter.caption!)
          ).toBe(true);
          expect(
            findByDataTestAttribute(
              'delete-selected-quick-filter-button',
              component
            ).length
          ).toBe(1);
        });

        it('should render available filters if selected quick filter was deleted', () => {
          const component = renderComponentWithSelectedQuickFilter(
            mockQuickFilters[0].caption!
          );

          findByDataTestAttribute(
            'delete-selected-quick-filter-button',
            component
          ).simulate('click');

          expect(
            findByDataTestAttribute('selected-quick-filter', component).length
          ).toBe(0);
          expect(findQuickFilterItems(component).length).toBe(
            mockQuickFilters.length
          );
        });

        it('should handle a input', () => {
          const component = renderComponentWithSelectedQuickFilter(
            mockQuickFilters[0].caption!
          );
          const filterValue = 'project';

          findInput(component).simulate('change', {
            target: { value: filterValue },
          });

          expect(findInput(component).prop('value')).toEqual(filterValue);
        });

        it('should not render quick filters on click by input', () => {
          const component = renderComponentWithSelectedQuickFilter(
            mockQuickFilters[0].caption!
          );

          findInput(component).simulate('blur');
          findInput(component).simulate('click');

          expect(
            findByDataTestAttribute('quick-filter-item', component).length
          ).toBe(0);
        });

        it('should create correct filter from quick filter on click by the "Add" button', () => {
          const addFilterSpy = jest.fn();
          const selectedQuickFilter = mockQuickFilters[0];
          const component = renderComponentWithSelectedQuickFilter(
            selectedQuickFilter.caption!,
            { onCreateFilter: addFilterSpy }
          );
          const quickFilterValue = 'project';

          findInput(component).simulate('change', {
            target: { value: quickFilterValue },
          });
          createFilterFromSelectedQuickFilter(component);

          expect(addFilterSpy).toBeCalled();
          const createdFilter: IFilterData = addFilterSpy.mock.calls[0][0];
          expect(createdFilter.name).toBe(selectedQuickFilter.propertyName);
          expect(createdFilter.value).toBe(quickFilterValue);
        });

        it('should reset a selected quick filter, a quick filter value', () => {
          const component = renderComponentWithSelectedQuickFilter(
            mockQuickFilters[0].caption!
          );

          findInput(component).simulate('change', {
            target: { value: 'project' },
          });
          createFilterFromSelectedQuickFilter(component);

          expect(
            findByDataTestAttribute('selected-quick-filter', component).length
          ).toBe(0);
          expect(findInput(component).prop('value')).toBe('');
        });
      });
    });
  });

  describe('when a quick filter is collapsed', () => {
    it('should reset a quick filter state when a quick filter become collapsed', () => {
      const selectedQuickFilter = mockQuickFilters[0];
      const component = renderComponentWithSelectedQuickFilter(
        selectedQuickFilter.caption!,
        { onCreateFilter: jest.fn() }
      );
      const quickFilterValue = 'project';
      findInput(component).simulate('change', {
        target: { value: quickFilterValue },
      });

      component.setProps({ isCollapsed: true });
      component.update();
      component.setProps({ isCollapsed: false });
      component.update();

      expect(
        findByDataTestAttribute('selected-quick-filter', component).length
      ).toBe(0);
      expect(findInput(component).prop('value')).toBeFalsy();
    });

    it('should expand the sidebar, show quick filters and focus on the input when the quick filter icon was clicked', () => {
      const onExpandSidebarSpy = jest.fn();
      const component = renderComponent({
        isCollapsed: true,
        onExpandSidebar: onExpandSidebarSpy,
      });

      findByDataTestAttribute(
        'collapsed-quick-filter-icon',
        component
      ).simulate('click');
      component.update();
      expect(onExpandSidebarSpy).toBeCalled();

      component.setProps({ isCollapsed: false });
      component.update();

      expect(findInput(component).length).toEqual(1);
      expect(checkInputIsFocused(component)).toBe(true);
      expect(
        findByDataTestAttribute('quick-filters', component).length
      ).toEqual(1);
    });
  });
});
