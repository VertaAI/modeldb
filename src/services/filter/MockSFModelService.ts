import { searchFilters } from 'store/filter';
import ModelRecord from '../../models/ModelRecord';
import MockSFService from './MockSFService';

export default class MockSFModelService extends MockSFService<ModelRecord> {
  public search(searchString: string): Promise<ModelRecord[]> {
    throw new Error('Method not implemented.');
  }
}
