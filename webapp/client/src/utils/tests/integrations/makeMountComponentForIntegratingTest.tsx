import { ConnectedRouter } from 'connected-react-router';
import { mount } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import { Store } from 'redux';

import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import setupIntegrationTest, {
  ISetupIntegrationTestSettings,
} from './setupIntegrationTest';

export interface IOptions {
  Component: any;
  settings?: ISetupIntegrationTestSettings;
  updateStoreBeforeMount?: (store: Store) => Promise<any>;
  updateStoreAfterMount?: (store: Store) => Promise<any>;
}

const makeMountComponentForIntegratingTest = async ({
  Component,
  settings,
  updateStoreBeforeMount,
  updateStoreAfterMount,
}: IOptions) => {
  const { store, dispatchSpy, history } = setupIntegrationTest(settings);

  if (updateStoreBeforeMount) {
    await updateStoreBeforeMount(store);
  }
  await flushAllPromises();

  const component = mount(
    <div id="root">
      <Provider store={store}>
        <ConnectedRouter history={history}>
          <Component />
        </ConnectedRouter>
      </Provider>
    </div>
  );

  if (updateStoreAfterMount) {
    await updateStoreAfterMount(store);
  }
  await flushAllPromises();

  return { component, store, history, dispatchSpy };
};

export default makeMountComponentForIntegratingTest;

// todo remove testing-library
import { configure } from '@testing-library/dom';
import { render, fireEvent, waitForElement } from '@testing-library/react';

configure({ testIdAttribute: 'data-test' });

export const makeComponentForIntegratingTest = async (
  Component: any,
  settings?: ISetupIntegrationTestSettings,
  updateStore?: (store: Store) => Promise<any>
) => {
  const { store, dispatchSpy, history } = setupIntegrationTest(settings);

  if (updateStore) {
    await updateStore(store);
  }
  await flushAllPromises();

  const component = render(
    <div id="root">
      <Provider store={store}>
        <ConnectedRouter history={history}>
          <Component />
        </ConnectedRouter>
      </Provider>
    </div>
  );
  return { component, store, history, dispatchSpy };
};
