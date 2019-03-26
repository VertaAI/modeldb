import { bind, debounce } from 'decko';
import { UnregisterCallback } from 'history';
import _ from 'lodash';
import * as React from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import { FilterContextPool, IFilterContext } from 'models/FilterContextPool';
import { IFilterData } from 'models/Filters';
import ModelRecord from 'models/ModelRecord';
import { Project } from 'models/Project';
import {
  addFilter,
  applyFilters,
  changeContext,
  editFilter,
  IFilterContextData,
  initContexts,
  removeFilter,
  search,
  suggestFilters
} from 'store/filter';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import Droppable from 'components/Droppable/Droppable';
import AppliedFilterItem from './AppliedFilterItem/AppliedFilterItem';
import FilterItem from './FilterItem/FilterItem';
import styles from './FilterSelect.module.css';

const contextMap: Map<string, string> = new Map();
contextMap.set('/', Project.name);
contextMap.set('', ModelRecord.name);

interface ILocalProps {
  appliedFilters: IFilterData[];
  ctx?: string;
  isFiltersSupporting: boolean;
  foundFilters?: IFilterData[];
  placeHolderText?: string;
}

interface ILocalState {
  isOpened: boolean;
  txt: string;
}

type AllProps = ILocalProps & IConnectedReduxProps & RouteComponentProps;

class FilterSelectComponent extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isOpened: false,
    txt: ''
  };
  private unlistenCallback: UnregisterCallback | undefined = undefined;

  public componentDidMount() {
    this.unlistenCallback = this.props.history.listen((location, action) => this.changeContext(location.pathname));
    this.props.dispatch(initContexts());
    this.changeContext(this.props.history.location.pathname);
  }

  public componentWillUnmount() {
    if (this.unlistenCallback) {
      this.unlistenCallback();
    }
  }

  public render() {
    const projectPage = this.props.history.location.pathname.split('/').length === 2 ? true : false;
    return (
      <div className={styles.root}>
        <div>
          <input
            className={styles.input}
            placeholder={this.props.placeHolderText}
            onChange={this.onChange}
            onClick={this.onShowPopup}
            defaultValue={this.state.txt}
            onKeyUp={this.onSearch}
          />
          <label className="fa fa-search" aria-hidden={true} />
          {this.renderPopup()}
        </div>
        {this.props.isFiltersSupporting && (
          <div>
            <Droppable type="filter" onDrop={this.onCreateFilter}>
              <div className={!projectPage ? styles.applied_filters : styles.applied_filters_proj}>
                {this.props.appliedFilters.map((filter, index) => (
                  <AppliedFilterItem
                    key={index}
                    data={filter}
                    onRemoveFilter={this.onRemoveFilter}
                    onChange={this.onSaveFilterData(index, this.props.ctx)}
                  />
                ))}
              </div>
            </Droppable>

            <div className={styles.apply_filters_button}>
              <button onClick={this.onApplyFilters}>Filter</button>
            </div>
          </div>
        )}
      </div>
    );
  }

  @bind
  private onChange(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({ ...this.state, txt: event.target.value });
    this.searchFilterSuggestions(event.target.value);
  }

  @bind
  private onApplyFilters() {
    if (this.props.ctx !== undefined) {
      this.props.dispatch(applyFilters(this.props.ctx, this.props.appliedFilters));
    }
  }

  @bind
  private onSearch(ev: React.KeyboardEvent<HTMLInputElement>) {
    if (ev.key === 'Enter') {
      if (this.props.ctx !== undefined) {
        this.props.dispatch(search(this.props.ctx, this.state.txt));
      }
    }
  }

  @bind
  private handleClickOutside(ev: MouseEvent) {
    this.setState({ ...this.state, isOpened: false });
  }

  @bind
  @debounce(500)
  private searchFilterSuggestions(txt: string) {
    this.props.dispatch(suggestFilters(txt));
    this.onShowPopup();
  }

  @bind
  private changeContext(pathname: string) {
    const ctx: IFilterContext | undefined = FilterContextPool.findContextByLocation(pathname);
    if (ctx !== undefined) {
      this.props.dispatch(changeContext(ctx.name));
    } else {
      this.props.dispatch(changeContext(undefined));
    }
  }

  @bind
  private renderPopup(): JSX.Element | false | undefined {
    return (
      this.props.foundFilters &&
      this.props.foundFilters.length > 0 &&
      this.state.isOpened && (
        <div className={styles.found_filters_popup}>
          {this.props.foundFilters!.map((filter, index) => (
            <FilterItem key={index} data={filter} onCreateFilter={this.onCreateFilter} />
          ))}
        </div>
      )
    );
  }

  @bind
  private onCreateFilter(data: IFilterData) {
    if (this.props.ctx !== undefined) {
      this.props.dispatch(addFilter(data, this.props.ctx));
    }
    this.setState({ ...this.state, isOpened: false });
  }

  @bind
  private onRemoveFilter(data: IFilterData) {
    if (this.props.ctx !== undefined) {
      this.props.dispatch(removeFilter(data, this.props.ctx));
    }
  }

  @bind
  private onSaveFilterData(index: number, ctx?: string) {
    return (data: IFilterData) => {
      if (ctx !== undefined) {
        this.props.dispatch(editFilter(index, data, ctx));
      }
    };
  }

  @bind
  private onShowPopup() {
    this.setState({
      ...this.state,
      isOpened: true
    });
  }
}

const mapStateToProps = ({ filters }: IApplicationState) => {
  if (filters.context !== undefined) {
    const fcData: IFilterContextData | undefined = filters.contexts[filters.context];
    if (fcData) {
      return {
        appliedFilters: fcData.appliedFilters,
        ctx: filters.context,
        foundFilters: filters.foundFilters,
        isFiltersSupporting: fcData.ctx.isFilteringSupport
      };
    }
  }

  return {
    appliedFilters: [],
    isFiltersSupporting: false,
    foundFilters: filters.foundFilters,
    ctx: undefined
  };
};

// export connect(mapStateToProps)(FilterSelect);
const filterSelect = withRouter(connect(mapStateToProps)(onClickOutside(FilterSelectComponent)));
// const filterSelect = connect(mapStateToProps)(FilterSelectComponent);
export { filterSelect as FilterSelect };
