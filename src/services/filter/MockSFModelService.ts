import { searchFilters } from 'store/filter';
import { Model } from '../../models/Model';
import MockSFService from './MockSFService';

export default class MockSFModelService extends MockSFService<Model> {
  public search(searchString: string): Promise<Model[]> {
    throw new Error('Method not implemented.');
  }
}
