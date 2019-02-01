import { searchFilters } from 'store/filter';
import Project from '../../models/Project';
import MockSFService from './MockSFService';

export default class MockSFProjectService extends MockSFService<Project> {
  public search(searchString: string): Promise<Project[]> {
    throw new Error('Method not implemented.');
  }
}
