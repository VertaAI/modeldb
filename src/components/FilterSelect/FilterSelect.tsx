import * as React from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { addFilter, removeFilter, searchFilters } from '../../store/filter/actions';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import FilterItem from './FilterItem';
import styles from './FilterSelect.module.css';

import { IFilterContextData } from 'store/filter';
import AppliedFilterItem from './AppliedFilterItem';

interface ILocalProps {
  placeHolderText?: string;
  foundFilters?: IFilterData[];
  appliedFilters: IFilterData[];
  isFiltersSupporting: boolean;
}

interface ILocalState {
  isOpened: boolean;
}

export interface IFilterData {
  propertyName: string;
  propertyValue: string;
}

type AllProps = ILocalProps & IConnectedReduxProps;

class FilterSelectComponent extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isOpened: false
  };

  private searchFiltersTimeout: number | undefined;
  public constructor(props: AllProps) {
    super(props);
    this.onChange = this.onChange.bind(this);

    this.renderPopup = this.renderPopup.bind(this);
    this.onShowPopup = this.onShowPopup.bind(this);
    this.onCreateFilter = this.onCreateFilter.bind(this);
    this.onRemoveFilter = this.onRemoveFilter.bind(this);
  }

  public onChange(event: React.ChangeEvent<HTMLInputElement>) {
    if (this.searchFiltersTimeout) {
      clearTimeout(this.searchFiltersTimeout);
    }

    const cb = ((txt: string) => {
      this.props.dispatch(searchFilters(txt));
      this.onShowPopup();
      this.searchFiltersTimeout = undefined;
    }).bind(this, event.target.value);

    this.searchFiltersTimeout = (setTimeout(cb, 300) as unknown) as number;
  }

  public render() {
    return (
      <div className={styles.root}>
        <div>
          <input className={styles.input} placeholder={this.props.placeHolderText} onChange={this.onChange} onClick={this.onShowPopup} />
          <label className="fa fa-search" aria-hidden={true} />
          {this.renderPopup()}
        </div>

        {this.props.isFiltersSupporting && (
          <div>
            <div className={styles.applied_filters}>
              {this.props.appliedFilters.map((filter, index) => (
                <AppliedFilterItem key={index} data={filter} onRemoveFilter={this.onRemoveFilter} />
              ))}
            </div>

            <div className={styles.apply_filters_button}>
              <button>Filter</button>
            </div>
          </div>
        )}
      </div>
    );
  }

  public handleClickOutside(ev: MouseEvent) {
    this.setState({ ...this.state, isOpened: false });
  }

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

  private onCreateFilter(data: IFilterData) {
    this.props.dispatch(addFilter(data));
  }

  private onRemoveFilter(data: IFilterData) {
    this.props.dispatch(removeFilter(data));
  }

  private onShowPopup() {
    this.setState({
      ...this.state,
      isOpened: true
    });
  }
}

const mapStateToProps = ({ filters }: IApplicationState) => {
  const currentContext: IFilterContextData | undefined =
    filters.context && filters.contexts.has(filters.context) ? filters.contexts.get(filters.context) : undefined;

  if (currentContext) {
    return {
      appliedFilters: currentContext.appliedFilters,
      foundFilters: filters.foundFilters,
      isFiltersSupporting: currentContext.isFiltersSupporting
    };
  }

  return { appliedFilters: [], isFiltersSupporting: false, foundFilters: filters.foundFilters };
};

// export connect(mapStateToProps)(FilterSelect);
const filterSelect = connect(mapStateToProps)(onClickOutside(FilterSelectComponent));
export { filterSelect as FilterSelect };
