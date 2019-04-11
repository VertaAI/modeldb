import axios from 'axios';

export class BaseDataService {
  public constructor() {
    axios.defaults.baseURL = '/api';
    axios.defaults.responseType = 'json';
  }
}
