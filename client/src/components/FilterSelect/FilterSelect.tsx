import { bind, debounce } from 'decko';
import { UnregisterCallback } from 'history';
import _ from 'lodash';
import * as React from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import Button from 'components/shared/Button/Button';
import Droppable from 'components/shared/Droppable/Droppable';
import TextInput from 'components/shared/TextInput/TextInput';
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
  selectCurrentContextData,
  selectFoundFilters,
  suggestFilters,
} from 'store/filter';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import AppliedFilterItem from './AppliedFilterItem/AppliedFilterItem';
import FilterItem from './FilterItem/FilterItem';
import styles from './FilterSelect.module.css';

const contextMap: Map<string, string> = new Map();
contextMap.set('/', Project.name);
contextMap.set('', ModelRecord.name);

interface ILocalProps {
  placeHolderText?: string;
}

interface IPropsFromState {
  appliedFilters: IFilterData[];
  ctxName?: string;
  isFiltersSupporting: boolean;
  foundFilters?: IFilterData[];
}

interface ILocalState {
  isOpened: boolean;
  txt: string;
}

type AllProps = ILocalProps &
  IPropsFromState &
  IConnectedReduxProps &
  RouteComponentProps;

class FilterSelectComponent extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isOpened: false,
    txt: '',
  };
  private unlistenCallback: UnregisterCallback | undefined = undefined;

  public componentDidMount() {
    this.unlistenCallback = this.props.history.listen((location, action) =>
      this.changeContext(location.pathname)
    );
    this.props.dispatch(initContexts());
    this.changeContext(this.props.history.location.pathname);
  }

  public componentWillUnmount() {
    if (this.unlistenCallback) {
      this.unlistenCallback();
    }
  }

  public render() {
    const projectPage =
      this.props.history.location.pathname.split('/').length === 2
        ? true
        : false;
    return (
      <div className={styles.root}>
        <div>
          <TextInput
            value={this.state.txt}
            placeholder={this.props.placeHolderText}
            icon="search"
            size="medium"
            onChange={this.onChange}
            onClick={this.onShowPopup}
          />
          {this.renderPopup()}
        </div>
        {this.props.isFiltersSupporting && (
          <div>
            <Droppable type="filter" onDrop={this.onCreateFilter}>
              <div
                className={
                  !projectPage
                    ? styles.applied_filters
                    : styles.applied_filters_proj
                }
              >
                {this.props.appliedFilters.map((filter, index) => (
                  <AppliedFilterItem
                    key={index}
                    data={filter}
                    onRemoveFilter={this.onRemoveFilter}
                    onChange={this.onSaveFilterData(index, this.props.ctxName)}
                  />
                ))}
              </div>
            </Droppable>

            <div className={styles.apply_filters_button}>
              <Button fullWidth={true} onClick={this.onApplyFilters}>
                Filter
              </Button>
            </div>
          </div>
        )}
      </div>
    );
  }

  @bind
  private onChange(value: string) {
    this.setState({ ...this.state, txt: value });
    this.searchFilterSuggestions(value);
  }

  @bind
  private onApplyFilters() {
    if (this.props.ctxName !== undefined) {
      this.props.dispatch(
        applyFilters(this.props.ctxName, this.props.appliedFilters)
      );
    }
  }

  @bind
  private onSearch(ev: React.KeyboardEvent<HTMLInputElement>) {
    if (ev.key === 'Enter') {
      if (this.props.ctxName !== undefined) {
        this.props.dispatch(search(this.props.ctxName, this.state.txt));
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
    const ctx:
      | IFilterContext
      | undefined = FilterContextPool.findContextByLocation(pathname);
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
            <FilterItem
              key={index}
              data={filter}
              onCreateFilter={this.onCreateFilter}
            />
          ))}
        </div>
      )
    );
  }

  @bind
  private onCreateFilter(data: IFilterData) {
    if (this.props.ctxName !== undefined) {
      this.props.dispatch(addFilter(data, this.props.ctxName));
    }
    this.setState({ ...this.state, isOpened: false });
  }

  @bind
  private onRemoveFilter(data: IFilterData) {
    if (this.props.ctxName !== undefined) {
      this.props.dispatch(removeFilter(data, this.props.ctxName));
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
      isOpened: true,
    });
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  const fcData: IFilterContextData | undefined = selectCurrentContextData(
    state
  );
  const foundFilters = selectFoundFilters(state);

  if (fcData) {
    return {
      appliedFilters: fcData.appliedFilters,
      ctxName: fcData.name,
      foundFilters,
      isFiltersSupporting: fcData.ctx.isFilteringSupport,
    };
  }

  return {
    foundFilters,
    appliedFilters: [],
    isFiltersSupporting: false,
    ctxName: undefined,
  };
};

const filterSelect = withRouter(
  connect(mapStateToProps)(onClickOutside(FilterSelectComponent))
);
export { filterSelect as FilterSelect };
