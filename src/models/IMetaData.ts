export interface IMetaData {
  propertyName?: string;
}

export abstract class MetaData {
  public static metaData: IMetaData[];
}
