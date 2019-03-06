import axios from 'axios';
import ServiceFactory from './ServiceFactory';

export class BaseDataService {
  public constructor() {
    const authenticationService = ServiceFactory.getAuthenticationService();

    const apiAddress = `${process.env.REACT_APP_BACKEND_API_PROTOCOL}://${process.env.REACT_APP_BACKEND_API_DOMAIN}${
      process.env.REACT_APP_BACKEND_API_PORT ? `:${process.env.REACT_APP_BACKEND_API_PORT}` : ''
    }`;

    axios.defaults.baseURL = apiAddress;
    axios.defaults.responseType = 'json';
    axios.defaults.headers = { 'Grpc-Metadata-bearer_access_token': authenticationService.accessToken, 'Grpc-Metadata-source': 'WebApp' };
  }
}
