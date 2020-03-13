import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import {
  IFilterData,
  PropertyType,
  IStringFilterData,
  IMetricFilterData,
  IExperimentNameFilterData,
} from 'core/features/filter/Model';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';
import { Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './InstantFilterItem.module.css';
import MetricFilterEditor from './MetricFilterEditor/MetricFilterEditor';
import getComparisonTypeView from './shared/getComparisonTypeView';
import RemoveButton from './shared/RemoveButton/RemoveButton';
import StringFilterEditor from './StringFilterEditor/StringFilterEditor';

interface ILocalProps {
  data: IFilterData;
  onRemoveFilter: (data: IFilterData) => void;
  onChange: (data: IFilterData) => void;
}

interface ILocalState {
  isEditorShown: boolean;
}

export default class InstantFilterItem extends React.Component<
  ILocalProps,
  ILocalState
> {
  public state: ILocalState = { isEditorShown: false };

  public render() {
    const { data } = this.props;
    const { isEditorShown } = this.state;
    const formatedFilterName = this.getFormatedFilterName(data);

    return (
      <div className={styles.root} data-test="filter-item">
        <div className={styles.actions}>
          <div className={styles.remove_button}>
            <RemoveButton onRemove={this.onClickRemove} />
          </div>
          <div
            className={styles.filter_text}
            title={formatedFilterName}
            data-test="filter-item-name"
          >
            {formatedFilterName}
          </div>
          <div className={styles.edit_button} onClick={this.onClickShowEditor}>
            <Icon type={isEditorShown ? 'caret-up' : 'caret-down'} />
          </div>
        </div>
        {isEditorShown && (
          <div className={styles.editor}>
            {(() => {
              const viewByType: Partial<Record<PropertyType, any>> = {
                [PropertyType.STRING]: (
                  <StringFilterEditor
                    onChange={this.onChange}
                    data={data as IStringFilterData}
                  />
                ),
                [PropertyType.METRIC]: (
                  <MetricFilterEditor
                    onChange={this.onChange}
                    data={data as IMetricFilterData}
                  />
                ),
                [PropertyType.EXPERIMENT_NAME]: (
                  <StringFilterEditor
                    onChange={this.onChange}
                    data={data as IExperimentNameFilterData}
                  />
                ),
              };
              return viewByType[data.type];
            })()}
          </div>
        )}
      </div>
    );
  }

  private getFormatedFilterName(filter: IFilterData): string {
    let result =
      filter.caption ||
      (filter.name.includes('.') ? filter.name.split('.')[1] : filter.name);
    if (filter.value !== undefined) {
      if (filter.type == PropertyType.METRIC) {
        const adjustedFilterValue = (() => {
          return withScientificNotationOrRounded(filter.value);
        })();
        result = `${result} ${getComparisonTypeView(
          filter.comparisonType
        )} ${adjustedFilterValue}`;
      } else {
        result = `${result}: ${filter.value}`;
      }
    }

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

  @bind
  private onClickShowEditor() {
    this.setState({ ...this.state, isEditorShown: !this.state.isEditorShown });
  }
}
