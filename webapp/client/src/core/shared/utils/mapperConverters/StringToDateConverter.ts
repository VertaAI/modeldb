import { JsonConverter, JsonCustomConvert } from 'json2typescript';

@JsonConverter
export class StringToDateConverter implements JsonCustomConvert<Date> {
  public serialize(date: Date): any {
    return date.getTime().toString();
  }
  public deserialize(date: any): Date {
    return new Date(Number(date));
  }
}
