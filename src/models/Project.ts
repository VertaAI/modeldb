import { BaseEntity } from './BaseEntity';
import { Model } from './Model';

export default class Project extends BaseEntity {
  private name: string = '';
  private description: string = '';
  private author: string = '';
  private models: Model[] = [];
  private creationDate: Date = new Date();

  public get Name(): string {
    return this.name;
  }

  public set Name(v: string) {
    this.name = v;
  }

  public get Description(): string {
    return this.description;
  }

  public set Description(v: string) {
    this.description = v;
  }

  public get Author(): string {
    return this.author;
  }

  public set Author(v: string) {
    this.author = v;
  }

  public get Models(): Model[] {
    return this.models;
  }

  public get CreationDate(): Date {
    return this.creationDate;
  }

  public set CreationDate(v: Date) {
    this.creationDate = v;
  }

}
