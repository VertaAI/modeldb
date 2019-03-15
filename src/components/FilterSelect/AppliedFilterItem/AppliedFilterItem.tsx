import * as React from 'react';
import { bind } from 'decko';
import { IFilterData, PropertyType } from '../../../models/Filters';
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

export default class AppliedFilterItem extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = { isEditorShown: false };

  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.ctrl}>
          <div className={styles.remove_button} onClick={this.onClickRemove}>
            {/* <i className="fa fa-filter" aria-hidden="true" /> */}x
          </div>
          <div className={styles.filter_text}>{`${this.props.data.name}${this.props.data.value ? `: ${this.props.data.value}` : ''}`}</div>
          <div className={styles.edit_button} onClick={this.onClickShowEditor}>
            <i className={this.state.isEditorShown ? 'fa fa-caret-up' : 'fa fa-caret-down'} aria-hidden="true" />
          </div>
        </div>
        {this.state.isEditorShown && (
          <div className={styles.editor}>
            {this.props.data.type === PropertyType.STRING ? (
              <StringFilterEditor onChange={this.props.onChange} data={this.props.data} />
            ) : this.props.data.type === PropertyType.NUMBER ? (
              <NumberFilterEditor onChange={this.props.onChange} data={this.props.data} />
            ) : this.props.data.type === PropertyType.METRIC ? (
              <MetricFilterEditor onChange={this.props.onChange} data={this.props.data} />
            ) : (
              ''
            )}
          </div>
        )}
      </div>
    );
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
