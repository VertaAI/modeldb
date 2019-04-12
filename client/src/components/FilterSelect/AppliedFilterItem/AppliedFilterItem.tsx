import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { ComparisonType, IFilterData, PropertyType } from 'models/Filters';

import Icon from 'components/shared/Icon/Icon';
import MetricFilterEditor from '../MetricFilterEditor/MetricFilterEditor';
import NumberFilterEditor from '../NumberFilterEditor/NumberFilterEditor';
import StringFilterEditor from '../StringFilterEditor/StringFilterEditor';
import styles from './AppliedFilterItem.module.css';

interface ILocalProps {
  data: IFilterData;
  onRemoveFilter?: (data: IFilterData) => void;
  onChange?: (data: IFilterData) => void;
}

interface ILocalState {
  isEditorShown: boolean;
}

export default class AppliedFilterItem extends React.Component<
  ILocalProps,
  ILocalState
> {
  public state: ILocalState = { isEditorShown: false };

  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.ctrl}>
          <div className={styles.remove_button} onClick={this.onClickRemove}>
            x
          </div>
          <div className={styles.filter_text}>
            {this.getFormatedFilterName(this.props.data)}
          </div>
          <div className={styles.edit_button} onClick={this.onClickShowEditor}>
            <Icon type={this.state.isEditorShown ? 'caret-up' : 'caret-down'} />
          </div>
        </div>
        {this.state.isEditorShown && (
          <div className={styles.editor}>
            {this.props.data.type === PropertyType.STRING ? (
              <StringFilterEditor
                onChange={this.props.onChange}
                data={this.props.data}
              />
            ) : this.props.data.type === PropertyType.NUMBER ? (
              <NumberFilterEditor
                onChange={this.props.onChange}
                data={this.props.data}
              />
            ) : this.props.data.type === PropertyType.METRIC ? (
              <MetricFilterEditor
                onChange={this.props.onChange}
                data={this.props.data}
              />
            ) : (
              ''
            )}
          </div>
        )}
      </div>
    );
  }

  private getFormatedFilterName(filter: IFilterData): string {
    let result = filter.caption;
    if (result === undefined) {
      result = filter.name;
    }

    if (filter.value !== undefined) {
      if (filter.type == PropertyType.METRIC) {
        let adjustedVal = String(Math.round(filter.value * 10000) / 10000);
        if (adjustedVal == '0') adjustedVal = filter.value.toExponential();
        let comparison = '';
        if (filter.comparisonType == ComparisonType.MORE) comparison = '>';
        else if (filter.comparisonType == ComparisonType.EQUALS) {
          comparison = '=';
        } else if (filter.comparisonType == ComparisonType.LESS) {
          comparison = '<';
        }
        result = `${result} ${comparison} ${adjustedVal}`;
      } else {
        result = `${result}: ${filter.value}`;
      }
    }

    return result;
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
