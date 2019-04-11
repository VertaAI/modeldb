/// <reference types="react-scripts" />
// tslint:disable-next-line:no-namespace
declare namespace NodeJS {
  // tslint:disable-next-line:interface-name
  interface ProcessEnv extends ProcessEnv {
    REACT_APP_USERNAME: string;
    REACT_APP_USER_EMAIL: string;
    REACT_APP_USE_API_DATA: string;
  }
}
