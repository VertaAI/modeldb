import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';

export const branchesAndTagsListHelpers = {
  findSelectedValue: (component: ReactWrapper) => {
    return findByDataTestAttribute(
      'branches-and-tags-list-selected-value',
      component
    ).text();
  },
};
