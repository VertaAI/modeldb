import { IRoute } from 'routes/makeRoute';

export class BreadcrumbItem {
  public path: string;
  public name: string;
  public shouldMatch: IRoute<any>;
  public previousItem?: BreadcrumbItem;

  public constructor(shouldMatch: IRoute<any>, path?: string, name?: string) {
    this.path = path || '';
    this.name = name || '';
    this.shouldMatch = shouldMatch;
  }
}
