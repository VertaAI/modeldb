import { bind } from 'decko';
import * as React from 'react';

import Checkbox from 'components/shared/Checkbox/Checkbox';
import TextInput from 'components/shared/TextInput/TextInput';
import { INumberFilterData } from 'models/Filters';
import { numberTo4Decimal } from 'utils/MapperConverters/NumberFormatter';

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
          <TextInput
            size="small"
            defaultValue={numberTo4Decimal(this.props.data.value).toString()}
            onBlur={this.onBlur}
            onKeyUp={this.onSubmit}
          />
        </div>
        <div className={styles.invert}>
          <Checkbox
            value={false}
            size="small"
            onChange={this.onInvertChanged}
          />
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
      const newData = {
        ...this.props.data,
        value: Number(event.currentTarget.value),
      };
      this.onSave(newData);
    }
  }

  @bind
  private onBlur(event: React.ChangeEvent<HTMLInputElement>) {
    const newData = { ...this.props.data, value: Number(event.target.value) };
    this.onSave(newData);
  }
}
