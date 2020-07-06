import { ReactWrapper } from 'enzyme';

import flushAllPromises from './flushAllPromises';

export async function flushAllPromisesFor(wrapper: ReactWrapper) {
  await flushAllPromises();
  wrapper.update();
}
