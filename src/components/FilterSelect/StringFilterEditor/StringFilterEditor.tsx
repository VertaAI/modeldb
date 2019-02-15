import * as React from 'react';
import { IStringFilterData } from '../../../models/Filters';
import styles from './StringFilterEditor.module.css';

interface ILocalProps {
  data: IStringFilterData;
  onChange?: (newData: IStringFilterData) => void;
}

export default class StringFilterEditor extends React.Component<ILocalProps> {
  public constructor(props: ILocalProps) {
    super(props);
    this.onSubmit = this.onSubmit.bind(this);
    this.onInvertChanged = this.onInvertChanged.bind(this);
    this.onSave = this.onSave.bind(this);
    this.onBlur = this.onBlur.bind(this);
  }

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
  private onSave(data: IStringFilterData) {
    if (this.props.onChange) {
      this.props.onChange(data);
    }
  }

  private onInvertChanged(event: React.ChangeEvent<HTMLInputElement>) {
    const newData = {
      ...this.props.data,
      invert: event.target.checked
    };
    this.onSave(newData);
  }
  private onSubmit(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      const newData = { ...this.props.data, value: event.currentTarget.value };
      this.onSave(newData);
    }
  }

  private onBlur(event: React.ChangeEvent<HTMLInputElement>) {
    const newData = { ...this.props.data, value: event.target.value };
    this.onSave(newData);
  }
}
