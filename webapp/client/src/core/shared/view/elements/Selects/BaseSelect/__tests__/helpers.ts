import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';
import { ReactWrapper } from 'enzyme';

export const makeSelectHelpers = <Value>(dataTest: string) => {
  const findInput = (component: ReactWrapper) =>
    findByDataTestAttribute(dataTest, component);
  const findOptions = (component: ReactWrapper) =>
    findByDataTestAttribute(`${dataTest}-option`, component) as ReactWrapper;
  const open = (component: ReactWrapper) => {
    findByDataTestAttribute(dataTest, component).simulate('click');
    component.update();
  };
  const changeValue = (component: ReactWrapper, value: Value) => {
    open(component);

    findOptions(component)
      .map(n => [(n as any).prop('data-value'), n])
      .find(n => n[0] === value)![1]
      .simulate('click');
    component.update();
  };
  const getInputText = (component: ReactWrapper) => {
    return findInput(component).text();
  };
  const getOptionsInfo = (component: ReactWrapper) => {
    open(component);
    return findOptions(component).map(option => ({
      label: option.text(),
      value: option.prop('data-value'),
    }));
  };
  const getSelectedValue = (component: ReactWrapper): Value => {
    return findByDataTestAttribute(`${dataTest}-root`, component).prop(
      'data-selected-value'
    );
  };
  const isDisabled = (component: ReactWrapper) => {
    return findByDataTestAttribute(`${dataTest}-root`, component).hasClass(
      'disabled'
    );
  };

  return {
    findInput,
    findOptions,
    open,
    changeValue,
    getInputText,
    getOptionsInfo,
    getSelectedValue,
    isDisabled,
  };
};
