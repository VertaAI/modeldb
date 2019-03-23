import axios from 'axios';
import ServiceFactory from './ServiceFactory';

export class BaseDataService {
  public constructor() {
    const authenticationService = ServiceFactory.getAuthenticationService();

    axios.defaults.baseURL = '/api';
    axios.defaults.responseType = 'json';
    axios.defaults.headers = { 'Grpc-Metadata-bearer_access_token': authenticationService.accessToken, 'Grpc-Metadata-source': 'WebApp' };
  }
}
