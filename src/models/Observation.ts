export interface IDataAttribute {
  key: string;
  value: number | string;
}

export interface IObservation {
  attribute: IDataAttribute;
  timestamp: Date;
}

export class Observation implements IObservation {
  public readonly attribute: IDataAttribute;
  public readonly timestamp: Date;
  constructor(attribute: IDataAttribute, timestamp: Date) {
    this.attribute = attribute;
    this.timestamp = timestamp;
  }
}
