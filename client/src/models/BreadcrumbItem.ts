export class BreadcrumbItem {
  public path: string;
  public name: string;
  public shouldMatch: RegExp;
  public previousItem?: BreadcrumbItem;

  public constructor(shouldMatch: RegExp, path?: string, name?: string) {
    this.path = path || '';
    this.name = name || '';
    this.shouldMatch = shouldMatch;
  }
}
