import { Popper } from '@material-ui/core';
import cn from 'classnames';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import {
  IFilterData,
  PropertyType,
  IStringFilterData,
  IMetricFilterData,
  IExperimentNameFilterData,
  IQuickFilter,
  IDateFilterData,
} from 'shared/models/Filters';
import Checkbox from 'shared/view/elements/Checkbox/Checkbox';
import ClickOutsideListener from 'shared/view/elements/ClickOutsideListener/ClickOutsideListener';
import { Icon } from 'shared/view/elements/Icon/Icon';

import FilterSelect from './FilterSelect/FilterSelect';
import styles from './InstantFilterItem.module.css';
import MetricFilterEditor from './MetricFilterEditor/MetricFilterEditor';
import StringFilterEditor from './StringFilterEditor/StringFilterEditor';
import DateFilterEditor from './DateFilterEditor/DateFilterEditor';

interface ILocalProps {
  data: IFilterData;
  onRemoveFilter: (data: IFilterData) => void;
  onChange: (data: IFilterData) => void;
  quickFilters: IQuickFilter[];
  onToggleFilterOpen: (filterId: IFilterData['id']) => void;
  isOpen: boolean;
}

export default class InstantFilterItem extends React.Component<ILocalProps> {
  private containerRef: React.RefObject<HTMLDivElement> = React.createRef();

  public componentDidMount() {
    /* 
      this fixes an error with ref and popper
      on the first load, popper renders withour anchorEl
      we forceUpdate here to force render and add correct ref to popper 
    */

    this.forceUpdate();
  }

  public render() {
    const { data, quickFilters, isOpen } = this.props;
    const formatedFilterName = this.getFormatedFilterName(data);

    const filterOptions = quickFilters.map(f => ({
      value: f,
      label: f.caption || f.propertyName,
    }));

    const filter = (
      <ClickOutsideListener onClickOutside={this.onClose}>
        <div
          className={cn(styles.root, {
            [styles.open]: isOpen,
            [styles.readonly]: !isOpen,
          })}
          data-test="filter-item-content"
        >
          <div className={styles.main_wrapper}>
            <div className={styles.main}>
              <Checkbox
                size="small"
                value={data.isActive}
                dataTest="active-filter-checkbox"
                onChange={this.onIsActiveChange}
              />

              <div className={styles.filter_name} onClick={this.onOpen}>
                <FilterSelect
                  options={filterOptions}
                  currentOption={{
                    value: data.name,
                    label: formatedFilterName,
                  }}
                  onChange={this.onChangeFilterName}
                  isReadonly={!isOpen || data.type === PropertyType.METRIC}
                />
              </div>
              <div className={styles.editor} onClick={this.onOpen}>
                {(() => {
                  const viewByType: Record<PropertyType, any> = {
                    [PropertyType.DATE]: (
                      <DateFilterEditor
                        onChange={this.onChange}
                        data={data as IDateFilterData}
                        isReadonly={!isOpen}
                      />
                    ),
                    [PropertyType.STRING]: (
                      <StringFilterEditor
                        onChange={this.onChange}
                        data={data as IStringFilterData}
                        isReadonly={!isOpen}
                      />
                    ),
                    [PropertyType.EXPERIMENT_NAME]: (
                      <StringFilterEditor
                        onChange={this.onChange}
                        data={data as IExperimentNameFilterData}
                        isReadonly={!isOpen}
                      />
                    ),
                    [PropertyType.METRIC]: (
                      <MetricFilterEditor
                        onChange={this.onChange}
                        data={data as IMetricFilterData}
                        isReadonly={!isOpen}
                      />
                    ),
                  };
                  return viewByType[data.type];
                })()}
              </div>
            </div>
          </div>
          {isOpen ? (
            <div
              className={styles.remove_button}
              onClick={this.onClickRemove}
              data-test="remove-filter"
            >
              <Icon type="close" />
            </div>
          ) : (
            <div
              className={styles.show_editor}
              onClick={this.onToggleOpen}
              data-test="open-filter"
            >
              <Icon type="caret-right" />
            </div>
          )}
        </div>
      </ClickOutsideListener>
    );

    return (
      <div className={styles.container} data-test="filter-item">
        <div
          style={{
            height: '100%',
            width: '1px',
            display: 'inline-block',
            position: 'absolute',
            left: 0,
            top: '-1px',
          }}
          ref={this.containerRef}
        />
        {isOpen && this.containerRef.current ? (
          <Popper
            anchorEl={this.containerRef.current}
            open={true}
            placement="right"
          >
            {filter}
          </Popper>
        ) : (
          filter
        )}
      </div>
    );
  }

  @bind
  private onToggleOpen() {
    const {
      onToggleFilterOpen,
      data: { id },
    } = this.props;
    setTimeout(() => {
      onToggleFilterOpen(id);
    });
  }

  @bind
  private onClose() {
    if (this.props.isOpen) {
      this.onToggleOpen();
    }
  }

  @bind
  private onOpen() {
    if (!this.props.isOpen) {
      this.onToggleOpen();
    }
  }

  @bind
  private onChangeFilterName({ value }: { value: IQuickFilter }) {
    if (value.type === PropertyType.STRING) {
      this.onChange({
        ...this.props.data,
        name: value.propertyName,
        type: value.type,
        operator: 'EQUALS',
        caption: value.caption,
        value: '',
      });
    } else {
      this.onChange({
        ...this.props.data,
        name: value.propertyName,
        type: value.type,
        operator: 'EQUALS',
        caption: value.caption,
        value: undefined,
      });
    }
  }

  @bind
  private onIsActiveChange(isActive: boolean) {
    this.props.onChange({ ...this.props.data, isActive });
  }

  @bind
  private getFormatedFilterName(filter: IFilterData): string {
    const result =
      filter.caption ||
      (filter.name.includes('.') ? filter.name.split('.')[1] : filter.name);

    return result;
  }

  // todo refactor it
  @bind
  private onChange(data: IFilterData) {
    if (!R.equals(data, this.props.data)) {
      this.props.onChange(data);
    }
  }

  @bind
  private onClickRemove() {
    if (this.props.onRemoveFilter) {
      this.props.onRemoveFilter(this.props.data);
    }
  }
}
