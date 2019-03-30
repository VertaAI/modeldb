import { JsonConverter, JsonCustomConvert } from 'json2typescript';

@JsonConverter
export class AnyToStringOrNumberConverter
  implements JsonCustomConvert<string | number> {
  public serialize(data: string | number): any {
    return data.toString();
  }
  public deserialize(data: any): string | number {
    const dataNumber: number = Number(data);
    if (isNaN(dataNumber)) {
      return data as string;
    }
    return dataNumber;
  }
}
