export class BreadcrumbItem {
  private path: string;
  private name: string;
  private shouldMatch: RegExp;
  private previousItem?: BreadcrumbItem;

  public constructor(shouldMatch: RegExp, path?: string, name?: string) {
    this.path = path || '';
    this.name = name || '';
    this.shouldMatch = shouldMatch;
  }

  public get ShouldMatch(): RegExp {
    return this.shouldMatch;
  }
  public set ShouldMatch(v: RegExp) {
    this.shouldMatch = v;
  }

  public get Path(): string {
    return this.path;
  }
  public set Path(v: string) {
    this.path = v;
  }

  public get Name(): string {
    return this.name;
  }
  public set Name(v: string) {
    this.name = v;
  }

  public get PreviousItem(): BreadcrumbItem | undefined {
    return this.previousItem;
  }
  public set PreviousItem(v: BreadcrumbItem | undefined) {
    this.previousItem = v;
  }
}
