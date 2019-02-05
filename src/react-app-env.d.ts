/// <reference types="react-scripts" />
declare namespace NodeJS {
  interface ProcessEnv extends ProcessEnv {
    REACT_APP_AUTH_CALLBACK_URL: string;
    REACT_APP_AUTH_CLIENT_ID: string;
    REACT_APP_AUTH_DOMAIN: string;
  }
}
