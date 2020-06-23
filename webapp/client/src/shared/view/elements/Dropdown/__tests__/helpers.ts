import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

import Preloader from '../../Preloader/Preloader';

export const makeDropdownHelpers = (
  findRoot: (component: ReactWrapper) => ReactWrapper
) => {
  const open = (component: ReactWrapper) =>
    findByDataTestAttribute('dropdown-button', findRoot(component)).simulate(
      'click'
    );

  const selectItemByText = (text: string, component: ReactWrapper) => {
    open(component);
    findByDataTestAttribute('dropdown-menu-item', findRoot(component))
      .filterWhere(item => item.text() === text)
      .first()
      .simulate('click');
  };

  const isLoading = (component: ReactWrapper) => {
    return findRoot(component).find(Preloader).length === 1;
  };

  return { open, selectItemByText, isLoading };
};
