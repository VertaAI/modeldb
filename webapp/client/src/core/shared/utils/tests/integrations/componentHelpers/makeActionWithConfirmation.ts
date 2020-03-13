import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';

import flushAllPromises from '../flushAllPromises';

const makeActionWithConfirmation = async (
  makeAction: () => void,
  component: ReactWrapper,
  withFlushingPromises = true
) => {
  makeAction();
  component.update();
  findByDataTestAttribute('confirm-ok-button', component).simulate('click');
  if (withFlushingPromises) {
    await flushAllPromises();
  }
  component.update();
};

export default makeActionWithConfirmation;
