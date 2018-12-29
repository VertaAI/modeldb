import {Store} from 'redux';

export abstract class BaseEntity {
  constructor(protected store: Store) {

  }
}
