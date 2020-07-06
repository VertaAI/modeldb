import { ReactWrapper } from 'enzyme';

import wait from './wait';

export default async function waitFor(component: ReactWrapper) {
  await wait();
  component.update();
}
