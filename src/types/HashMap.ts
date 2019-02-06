import { string } from 'prop-types';

export class HashMap<T> {
  protected items: { [key: string]: T };

  constructor(map?: HashMap<T>) {
    this.items = {};
    if (map) {
      for (const key in map.items) {
        if (map.items.hasOwnProperty(key)) {
          const element = map.items[key];
          this.set(key, element);
        }
      }
    }
  }

  public set(key: string, value: T): void {
    this.items[key] = value;
  }

  public has(key: string): boolean {
    return key in this.items;
  }

  public get(key: string): T {
    return this.items[key];
  }
}
