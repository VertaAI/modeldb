import * as React from 'react';
import { bind } from 'decko';

import { IStringFilterData } from 'models/Filters';

import styles from './StringFilterEditor.module.css';

interface ILocalProps {
  data: IStringFilterData;
  onChange?: (newData: IStringFilterData) => void;
}

export default class StringFilterEditor extends React.Component<ILocalProps> {
  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.input}>
          <input type="text" defaultValue={this.props.data.value} onBlur={this.onBlur} onKeyUp={this.onSubmit} />
        </div>
        <div className={styles.invert}>
          <input type="checkbox" defaultChecked={this.props.data.invert} onChange={this.onInvertChanged} />
          <label>invert</label>
        </div>
      </div>
    );
  }

  @bind
  private onSave(data: IStringFilterData) {
    if (this.props.onChange) {
      this.props.onChange(data);
    }
  }

  @bind
  private onInvertChanged(event: React.ChangeEvent<HTMLInputElement>) {
    const newData = {
      ...this.props.data,
      invert: event.target.checked
    };
    this.onSave(newData);
  }

  @bind
  private onSubmit(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      const newData = { ...this.props.data, value: event.currentTarget.value };
      this.onSave(newData);
    }
  }

  @bind
  private onBlur(event: React.ChangeEvent<HTMLInputElement>) {
    const newData = { ...this.props.data, value: event.target.value };
    this.onSave(newData);
  }
}
