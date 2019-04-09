import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';

import { IFilterContextData } from 'store/filter';
import { IApplicationState, IThunkActionDependencies } from 'store/store';

import { IFilterData } from './Filters';
import { IMetaData } from './IMetaData';

export interface IFilterContext {
  isFilteringSupport: boolean;
  name: string;
  getMetadata(): IMetaData[];
  onSearch(
    text: string,
    dispatch: ThunkDispatch<
      IApplicationState,
      IThunkActionDependencies,
      AnyAction
    >
  ): void;
  onApplyFilters(
    filters: IFilterData[],
    dispatch: ThunkDispatch<
      IApplicationState,
      IThunkActionDependencies,
      AnyAction
    >
  ): void;
  isValidLocation(location: string): boolean;
}

class FilterContextPool {
  private contexts: { [index: string]: IFilterContext } = {};

  public registerContext(context: IFilterContext) {
    if (context.name.length === 0) {
      throw new Error('Bad context name');
    }
    if (this.contexts[context.name] !== undefined) {
      throw new Error('Context with such name is already existed');
    }

    this.contexts[context.name] = context;
  }

  public unregisterContext(name: string) {
    if (name.length === 0) {
      throw new Error('Bad context name');
    }

    if (this.contexts[name] === undefined) {
      throw new Error('Context is not registered');
    }

    delete this.contexts[name];
  }

  public initContextsData(): IFilterContextData[] {
    const result: IFilterContextData[] = [];
    for (const prop in this.contexts) {
      if (this.contexts.hasOwnProperty(prop)) {
        const ctx = this.contexts[prop];

        const resultItem = { ctx, appliedFilters: [], name: ctx.name };
        const confData = localStorage.getItem(`${ctx.name}_filter`);
        if (confData !== null) {
          resultItem.appliedFilters = JSON.parse(confData);
        }
        result.push(resultItem);
      }
    }
    return result;
  }

  public findContextByLocation(location: string): IFilterContext | undefined {
    for (const ctxName in this.contexts) {
      if (this.contexts.hasOwnProperty(ctxName)) {
        const ctx = this.contexts[ctxName];
        if (ctx.isValidLocation(location)) {
          return ctx;
        }
      }
    }
    return undefined;
  }

  public getContextByName(name: string): IFilterContext {
    if (name.length === 0) {
      throw new Error('Bad context name');
    }

    if (this.contexts[name] === undefined) {
      throw new Error('Context is not registered');
    }

    return this.contexts[name];
  }
  public hasContext(name: string) {
    return this.contexts[name] !== undefined;
  }
}

// singleton
const filterContextPool = new FilterContextPool();
export { filterContextPool as FilterContextPool };
