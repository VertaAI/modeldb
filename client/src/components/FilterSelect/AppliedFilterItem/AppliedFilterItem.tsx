import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { IFilterData, PropertyType } from 'models/Filters';

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
            {/* <i className={cn('fa', 'fa-filter')} aria-hidden="true" /> */}
          </div>
          <div className={styles.filter_text}>
            {this.getFormatedFilterName(this.props.data)}
          </div>
          <div className={styles.edit_button} onClick={this.onClickShowEditor}>
            <i
              className={cn('fa', {
                'fa-caret-down': !this.state.isEditorShown,
                'fa-caret-up': this.state.isEditorShown,
              })}
              aria-hidden="true"
            />
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
      result = `${result}: ${filter.value}`;
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
