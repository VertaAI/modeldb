/// <reference types="react-scripts" />
// tslint:disable-next-line:no-namespace
declare namespace NodeJS {
  // tslint:disable-next-line:interface-name
  interface ProcessEnv extends ProcessEnv {
    REACT_APP_BACKEND_API_PROTOCOL: string;
    REACT_APP_BACKEND_API_DOMAIN: string;
    REACT_APP_BACKEND_API_PORT: string;
    REACT_APP_USE_API_DATA: string;
    REACT_APP_USERNAME: string;
    REACT_APP_USER_EMAIL: string;
  }
}
