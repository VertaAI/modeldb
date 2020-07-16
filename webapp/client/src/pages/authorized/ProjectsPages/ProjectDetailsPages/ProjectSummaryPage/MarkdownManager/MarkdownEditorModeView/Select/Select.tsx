import { bind } from 'decko';
import * as React from 'react';

import styles from './Select.module.css';

interface ILocalProps<T> {
  title: string;
  value: T;
  options: Array<IOption<T>>;
  onChange(value: T): void;
}

interface IOption<T> {
  value: T;
  label: string;
}

class Select<T extends string | number> extends React.PureComponent<
  ILocalProps<T>
> {
  public render() {
    const { title, value, options } = this.props;
    return (
      <select className={styles.select} value={value} onChange={this.onChange}>
        <optgroup label={title}>
          {options.map((option) => (
            <option className={styles.option} value={option.value}>
              {option.label}
            </option>
          ))}
        </optgroup>
      </select>
    );
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLSelectElement>) {
    this.props.onChange(e.currentTarget.value as any);
  }
}

export default Select;
