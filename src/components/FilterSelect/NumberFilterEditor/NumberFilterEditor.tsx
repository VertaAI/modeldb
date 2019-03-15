import * as React from 'react';
import { bind } from 'decko';
import { INumberFilterData } from '../../../models/Filters';
import styles from './NumberFilterEditor.module.css';

interface ILocalProps {
  data: INumberFilterData;
  onChange?: (newData: INumberFilterData) => void;
}

export default class NumberFilterEditor extends React.Component<ILocalProps> {
  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.input}>
          <input type="number" defaultValue={this.props.data.value.toString()} onBlur={this.onBlur} onKeyUp={this.onSubmit} />
        </div>
        <div className={styles.invert}>
          <input type="checkbox" defaultChecked={this.props.data.invert} />
          <label>invert</label>
        </div>
      </div>
    );
  }

  @bind
  private onClick() {
    if (this.props.onChange) {
      this.props.onChange(this.props.data);
    }
  }

  @bind
  private onSave(data: INumberFilterData) {
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
      const newData = { ...this.props.data, value: Number(event.currentTarget.value) };
      this.onSave(newData);
    }
  }

  @bind
  private onBlur(event: React.ChangeEvent<HTMLInputElement>) {
    const newData = { ...this.props.data, value: Number(event.target.value) };
    this.onSave(newData);
  }
}
