import { IDataService } from './IDataService';
import MockDataService from './MockDataService';

export default class ServiceFactory {
  public static getDataService(): IDataService {
    return new MockDataService();
  }
}
