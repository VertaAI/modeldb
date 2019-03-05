export interface IDataAttribute {
  key: string;
  value: string;
}

export interface IObservation {
  attribute: IDataAttribute;
  timestamp: string;
}

export class Observation implements IObservation {
  public readonly attribute: IDataAttribute;
  public readonly timestamp: string;
  constructor(attribute: IDataAttribute, timestamp: string) {
    this.attribute = attribute;
    this.timestamp = timestamp;
  }
}
