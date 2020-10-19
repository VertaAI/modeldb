import { createBrowserHistory } from 'history';
import { Store } from 'redux';

export interface ISetupIntegrationTestSettings<State> {
  initialState?: Partial<State>;
  pathname?: string;
}

const makeSetupIntegrationTest = <State>({
  configureStore,
}: {
  configureStore: any;
}) => {
  return (settings?: ISetupIntegrationTestSettings<State>) => {
    const dispatchSpy = jest.fn(() => ({}));
    const dispatchSpyMiddleware = (store: any) => (next: any) => (
      action: any
    ) => {
      (dispatchSpy as any)(action);
      return next(action);
    };

    const history = createBrowserHistory();
    if (settings && settings.pathname) {
      history.push(settings.pathname);
    }
    const emptyStore = (configureStore as any)(
      history,
      (settings && settings.initialState) || undefined,
      [dispatchSpyMiddleware]
    );
    const store: Store<State> = emptyStore;

    return { store, dispatchSpy, history };
  };
};

export default makeSetupIntegrationTest;
