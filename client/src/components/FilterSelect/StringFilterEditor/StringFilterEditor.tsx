import { bind } from 'decko';
import * as React from 'react';

import Checkbox from 'components/shared/Checkbox/Checkbox';
import { IStringFilterData } from 'models/Filters';

import TextInput from 'components/shared/TextInput/TextInput';
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
          <TextInput
            defaultValue={this.props.data.value}
            size="small"
            onKeyUp={this.onSubmit}
            onBlur={this.onBlur}
          />
        </div>
        <div className={styles.invert}>
          <Checkbox
            value={this.props.data.invert}
            size="small"
            onChange={this.onInvertChanged}
          />
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
  private onInvertChanged(invert: boolean) {
    const newData = {
      ...this.props.data,
      invert,
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
