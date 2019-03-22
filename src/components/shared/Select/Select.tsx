import * as React from 'react';
import { bind } from 'decko';

import styles from './Select.module.css';

interface IProps<T> {
  options: Array<IOption<T>>;
  value: T;
  onChange(value: T): void;
}

interface IOption<T> {
  value: T;
  label: string;
}

class Select<T> extends React.PureComponent<IProps<T>> {
  public render() {
    const { value, options } = this.props;
    return (
      <select className={styles.select} value={String(value)} onChange={this.onChange}>
        {options.map(({ value, label }) => (
          <option value={String(value)} key={String(value)}>
            {label}
          </option>
        ))}
      </select>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLSelectElement>) {
    const realValue = this.props.options.find(option => String(option.value) === e.target.value)!.value;
    this.props.onChange(realValue);
  }
}

export default Select;
