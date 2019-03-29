export class BreadcrumbItem {
  public path: string;
  public name: string;
  public shouldMatch: string;
  public previousItem?: BreadcrumbItem;

  public constructor(shouldMatch: string, path?: string, name?: string) {
    this.path = path || '';
    this.name = name || '';
    this.shouldMatch = shouldMatch;
  }
}
