import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

import styles from '../FilterSelect.module.css';

export const makeFilterSelectHelpers = (
  getFilterSelect: () => ReactWrapper
) => {
  const getDisplayedSelectedOption = () => {
    return findByDataTestAttribute(
      'filter-select-value',
      getFilterSelect()
    ).text();
  };
  const isOpened = () => {
    return (
      findByDataTestAttribute('filter-select-options', getFilterSelect())
        .length === 1
    );
  };

  const open = () => getFilterSelect().simulate('click');

  const isDisabled = () => {
    return getFilterSelect().hasClass(styles.readonly);
  };

  const changeOption = (optionLabel: string) => {
    open();
    return getFilterSelect()
      .find(`[data-target="quick-filter-item"]`)
      .findWhere(n => n.text() === optionLabel)
      .first()
      .simulate('click');
  };

  return {
    getDisplayedSelectedOption,
    isOpened,
    isDisabled,
    open,
    changeOption,
  };
};
