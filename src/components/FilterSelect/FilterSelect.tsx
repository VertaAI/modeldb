import * as React from 'react';
import { connect } from 'react-redux';
import { searchFilters } from '../../store/filter/actions';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import FilterItem from './FilterItem';
import styles from './FilterSelect.module.css';
import dragDropImg from './images/drag-and-drop.png';

import AppliedFilterItem from './AppliedFilterItem';

interface ILocalProps {
  placeHolderText?: string;
  foundFilters?: IFilterData[];
  isFiltersSupport: boolean;
  appliedFilters: IFilterData[];
}

interface ILocalState {
  isFocused: boolean;
}

export interface IFilterData {
  propertyName: string;
  propertyValue: string;
}

type AllProps = ILocalProps & IConnectedReduxProps;
class FilterSelectComponent extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isFocused: false
  };
  public constructor(props: AllProps) {
    super(props);
    this.onChange = this.onChange.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.onBlur = this.onBlur.bind(this);

    this.renderPopup = this.renderPopup.bind(this);
  }

  public onChange(event: React.ChangeEvent<HTMLInputElement>) {
    this.props.dispatch(searchFilters(event.target.value));
  }

  public onFocus() {
    this.setState({ ...this.state, isFocused: true });
  }

  public onBlur() {
    this.setState({ ...this.state, isFocused: false });
  }
  public render() {
    return (
      <div className={styles.root}>
        <div>
          <input
            className={styles.input}
            placeholder={this.props.placeHolderText}
            onChange={this.onChange}
            onFocus={this.onFocus}
            onBlur={this.onBlur}
          />
          <label className="fa fa-search" aria-hidden={true} />
          {this.renderPopup()}
        </div>

        {this.props.isFiltersSupport && (
          <div>
            <div className={styles.applied_filters}>
              {this.props.appliedFilters.map((filter, index) => (
                <AppliedFilterItem key={index} data={filter} />
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

  private renderPopup(): JSX.Element | false | undefined {
    return (
      this.props.foundFilters &&
      this.props.foundFilters.length > 0 &&
      this.state.isFocused && (
        <div className={styles.found_filters_popup}>
          {this.props.foundFilters!.map((filter, index) => (
            <FilterItem
              key={index}
              PropertyName={filter.propertyName}
              PropertyValue={filter.propertyValue}
              onCreateFilter={this.onCreateFilter}
            />
          ))}
        </div>
      )
    );
  }

  private onCreateFilter(data: IFilterData) {
    console.log(data);
  }
}

const mapStateToProps = ({ filters }: IApplicationState) => ({
  appliedFilters: filters.appliedFilters,
  foundFilters: filters.foundFilters,
  isFiltersSupport: filters.isFiltersSupporting
});

// export connect(mapStateToProps)(FilterSelect);
const filterSelect = connect(mapStateToProps)(FilterSelectComponent);
export { filterSelect as FilterSelect };
