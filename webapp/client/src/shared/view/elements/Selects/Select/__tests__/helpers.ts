import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

export const makeSelectHelpers = (name: string) => {
  const getSelect = (component: ReactWrapper) => {
    return component.find(`[data-test="${name}"]`);
  };

  const findRoot = getSelect;
  const findOptions = (component: ReactWrapper) => {
    return component.find('.react-select__option .optionLabel');
  };
  const findOptionsText = (component: ReactWrapper) => {
    open(component);
    return findOptions(component).map(node => node.text());
  };
  const open = (component: ReactWrapper) => {
    component.find(`[data-select-name="${name}"]`).simulate('click');
    component.update();
  };
  const selectOptionByLabel = (text: string, component: ReactWrapper) => {
    open(component);
    findOptions(component)
      .map(optionNode => ({ node: optionNode, text: optionNode.text() }))
      .find(
        option => option.text.trim().toLowerCase() === text.trim().toLowerCase()
      )!
      .node.simulate('click');
    component.update();
  };
  const findSelectedOptionLabel = (component: ReactWrapper) => {
    return getSelect(component)
      .find('.react-select__single-value')
      .last()
      .text();
  };
  const isDisabled = (component: ReactWrapper) => {
    return Boolean(
      getSelect(component)
        .first()
        .prop('isDisabled')
    );
  };

  return {
    findRoot,
    findOptions,
    findOptionsText,
    open,
    selectOptionByLabel,
    findSelectedOptionLabel,
    isDisabled,
  };
};
