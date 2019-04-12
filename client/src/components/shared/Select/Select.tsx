import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import onClickOutside, { HandleClickOutside } from 'react-onclickoutside';

import Icon from '../Icon/Icon';
import styles from './Select.module.css';

interface ILocalProps<T> {
  options: Array<IOption<T>>;
  value: T;
  onChange(value: T): void;
}

interface IOption<T> {
  value: T;
  label: string;
}

interface ILocalState {
  isOpen: boolean;
}

class Select<T> extends React.PureComponent<ILocalProps<T>, ILocalState>
  implements HandleClickOutside<any> {
  public state: ILocalState = { isOpen: false };

  public render() {
    const { value, options } = this.props;
    const { isOpen } = this.state;

    const selectedOption = options.find(option => option.value === value)!;

    return (
      <div className={cn(styles.select, { [styles.select_open]: isOpen })}>
        <div className={styles.input} onClick={this.onOpen}>
          {selectedOption.label}
          <Icon type="caret-down" className={styles.arrow} />
        </div>
        <div className={styles.options}>
          {options.map((option, i) => (
            <div
              className={cn(styles.option, {
                [styles.option_selected]: option.value === value,
              })}
              key={i}
              onClick={this.makeOnChange(option.value)}
            >
              {option.label}
            </div>
          ))}
        </div>
      </div>
    );
  }

  public handleClickOutside() {
    this.onClose();
  }

  @bind
  private onOpen() {
    this.setState({ isOpen: true });
  }

  @bind
  private onClose() {
    this.setState({ isOpen: false });
  }

  @bind
  private makeOnChange(value: T) {
    return () => {
      this.props.onChange(value);
      this.onClose();
    };
  }
}

export default onClickOutside(Select);
