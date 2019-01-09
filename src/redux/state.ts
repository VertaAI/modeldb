import {Project} from '../models/Project';

export class ModelDBState {
  public projects: Project[] = [];
}

export const initialState = new ModelDBState();
