import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { Dispatch, bindActionCreators } from 'redux';

import { IFilterData } from 'shared/models/Filters';
import Droppable from 'shared/view/elements/Droppable/Droppable';
import { Icon } from 'shared/view/elements/Icon/Icon';

import {
  resetCurrentContext,
  setContext,
  removeFilterFromCurrentContext,
  addFilterToCurrentContext,
  editFilterInCurrentContext,
} from '../../store/actions';
import { selectCurrentContextData } from '../../store/selectors';
import {
  IFilterContext,
  IFilterContextData,
  IFilterRootState,
} from '../../store/types';
import AddFilter from './AddFilter/AddFilter';
import styles from './FilterManager.module.css';
import InstantFilterItem from './InstantFilterItem/InstantFilterItem';

interface ILocalProps {
  isCollapsed: boolean;
  context: IFilterContext;
  title: string;
  onExpandSidebar(): void;
}

const mapStateToProps = (state: IFilterRootState) => {
  const currentContextData:
    | IFilterContextData
    | undefined = selectCurrentContextData(state);
  const quickFilters = currentContextData
    ? currentContextData.ctx.quickFilters
    : [];

  return {
    filters: currentContextData ? currentContextData.filters : [],
    quickFilters,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      setContext,
      resetCurrentContext,
      addFilterToCurrentContext,
      removeFilterFromCurrentContext,
      editFilterInCurrentContext,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  RouteComponentProps;

interface ILocalState {
  openedFilterId: string | null;
}

class FilterManager extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = { openedFilterId: null };

  public componentDidMount() {
    this.props.setContext(this.props.context);
  }

  public componentWillUnmount() {
    this.props.resetCurrentContext();
  }

  public render() {
    const { isCollapsed, filters, onExpandSidebar, title } = this.props;

    return (
      <div
        className={cn(styles.root, {
          [styles.collapsed]: isCollapsed,
        })}
        data-test="filters"
      >
        {isCollapsed && (
          <div className={styles.filtersCollapsed} onClick={onExpandSidebar}>
            <Icon type="filter-light" className={styles.filtersIcon} />
            <div className={styles.filtersCollapsedCounter}>
              {filters.length}
            </div>
          </div>
        )}
        <div className={styles.filters}>
          <div className={styles.title}>
            <Icon type="filter-light" className={styles.filtersIcon} />
            {title}
          </div>
          <Droppable type="filter" onDrop={this.onCreateFilter}>
            <div
              className={cn(styles.dragAndDropArea)}
              data-test="filter-items-area"
            >
              <div className={styles.dragAndDropAreaPlaceholder}>
                <Icon type="drag-and-drop" className={styles.dragIcon} />
                <div className={styles.placeholderText}>
                  Drag and drop to filter
                </div>
              </div>
            </div>
          </Droppable>
          <div>
            {filters.map((filter, index) => (
              <InstantFilterItem
                key={index}
                data={filter}
                quickFilters={this.props.quickFilters}
                onRemoveFilter={this.onRemoveFilter}
                onChange={this.OnEditFilterData}
                isOpen={filter.id === this.state.openedFilterId}
                onToggleFilterOpen={this.onToggleFilterOpen}
              />
            ))}
            <AddFilter
              quickFilters={this.props.quickFilters}
              onCreateFilter={this.onCreateFilter}
            />
          </div>
        </div>
      </div>
    );
  }

  @bind
  private onToggleFilterOpen(filterId: string) {
    this.setState({
      openedFilterId: this.state.openedFilterId === filterId ? null : filterId,
    });
  }

  @bind
  private onCreateFilter(data: IFilterData) {
    this.onToggleFilterOpen(data.id);
    this.props.addFilterToCurrentContext(data);
  }

  @bind
  private onRemoveFilter(data: IFilterData) {
    this.props.removeFilterFromCurrentContext(data);
  }

  @bind
  private OnEditFilterData(data: IFilterData) {
    this.props.editFilterInCurrentContext(data);
  }
}

export type IFilterManagerLocalProps = ILocalProps;
export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(FilterManager)
);
