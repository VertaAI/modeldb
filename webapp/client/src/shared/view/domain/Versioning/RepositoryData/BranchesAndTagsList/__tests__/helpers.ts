import { ReactWrapper } from 'enzyme';

import {
  findByDataTestAttribute,
  makeInputHelpers,
} from 'shared/utils/tests/react/helpers';

export const createBranchesAndTagsListHelpers = (dataTest: string) => {
  const getInstance = (component: ReactWrapper) =>
    findByDataTestAttribute(dataTest, component);
  const isMenuOpen = (component: ReactWrapper) => {
    return findByDataTestAttribute(
      'branches-and-tags-menu',
      getInstance(component)
    ).hasClass('opened');
  };
  const openMenu = (component: ReactWrapper) => {
    if (!isMenuOpen(component)) {
      findByDataTestAttribute(
        'branches-and-tags-summary-button',
        getInstance(component)
      ).simulate('click');
      component.update();
    }
  };
  return {
    openMenu,
    isMenuOpen,
    getDisplayedItems: (component: ReactWrapper) => {
      openMenu(component);
      return findByDataTestAttribute(
        'branches-and-tags-list-item',
        getInstance(component)
      ).map(n => n.text());
    },
    findSelectedValue: (component: ReactWrapper) => {
      return findByDataTestAttribute(
        'branches-and-tags-list-selected-value',
        getInstance(component)
      ).text();
    },
    filterField: makeInputHelpers(node =>
      node.find(`input[name="branche-and-tags-list-filter"]`)
    ),
    openTab: (tab: 'branches' | 'tags', component: ReactWrapper) => {
      openMenu(component);
      findByDataTestAttribute(
        'branche-and-tags-list-tab',
        getInstance(component)
      )
        .findWhere(n => n.text().toLowerCase() === tab)
        .first()
        .simulate('click');
      component.update();
    },
    changeCommitPointer: (item: string, component: ReactWrapper) => {
      openMenu(component);
      findByDataTestAttribute(
        'branches-and-tags-list-item',
        getInstance(component)
      )
        .findWhere(n => n.text() === item)
        .first()
        .simulate('click');
      component.update();
    },
  };
};
