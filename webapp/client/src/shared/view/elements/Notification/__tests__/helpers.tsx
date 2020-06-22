import * as React from 'react';
import { ReactWrapper } from 'enzyme';

import waitFor from 'shared/utils/tests/integrations/waitFor';

interface ITestNotification {
  type: 'success' | 'error';
  content: string;
}

export const getDisplayedNotifications = async (
  component: ReactWrapper
): Promise<ITestNotification[]> => {
  await waitFor(component);
  await waitFor(component);
  return component.find('.Toastify__toast-body').map(n => ({
    type: 'error',
    content: n.text(),
  }));
};

export const closeAllNotifications = async (component: ReactWrapper) => {
  // find the better solution
  component.unmount();
  component.mount();
};
