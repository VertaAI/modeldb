import { FilterContextPool, IFilterContext } from './FilterContextPool';
import { IFilterData, PropertyType } from './Filters';

export default class Project {
  private id: string = '';
  private name: string = '';
  private description: string = '';
  private dateCreated: Date = new Date();
  private dateUpdated: Date = new Date();
  private author?: string;
  private tags: string[] = [];

  public get Id(): string {
    return this.id;
  }

  public set Id(v: string) {
    this.id = v;
  }

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

  public get DateCreated(): Date {
    return this.dateCreated;
  }

  public set DateCreated(v: Date) {
    this.dateCreated = v;
  }

  public get DateUpdated(): Date {
    return this.dateUpdated;
  }

  public set DateUpdated(v: Date) {
    this.dateUpdated = v;
  }

  public get Author(): string | undefined {
    return this.author;
  }

  public set Author(v: string | undefined) {
    this.author = v;
  }

  public get Tags(): string[] {
    return this.tags;
  }

  public set Tags(v: string[]) {
    this.tags = v;
  }
}

FilterContextPool.registerContext({
  metadata: [
    { propertyName: 'Name', type: PropertyType.STRING },
    { propertyName: 'Description', type: PropertyType.STRING },
    { propertyName: 'Tag', type: PropertyType.STRING },
    { propertyName: 'Id', type: PropertyType.NUMBER },
    { propertyName: 'acc', type: PropertyType.METRIC }
  ],

  isFilteringSupport: true,
  isValidLocation: (location: string) => {
    return location === '/';
  },
  name: Project.name,
  onApplyFilters: (filters: IFilterData[]) => {
    console.log(`Apply: ${filters.length}`);
  },
  onSearch: (text: string) => {
    console.log(`Search: ${text}`);
  }
});
