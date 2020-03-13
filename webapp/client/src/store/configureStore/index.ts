let configureStore: any;

if (process.env.NODE_ENV === 'production') {
  // tslint:disable-next-line:no-var-requires
  configureStore = require('./configureStore.production').default;
} else {
  // tslint:disable-next-line:no-var-requires
  configureStore = require('./configureStore.development').default;
}

export default configureStore;
