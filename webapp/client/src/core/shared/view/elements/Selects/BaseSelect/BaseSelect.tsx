import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import {
  Manager as PopperManager,
  Popper,
  Reference as ReferenceForPopper,
  RefHandler,
} from 'react-popper';

import ClickOutsideListener from 'core/shared/view/elements/ClickOutsideListener/ClickOutsideListener';

import styles from './BaseSelect.module.css';

interface ILocalProps<T> {
  rootClassname?: string;
  value: T;
  options: Array<{ value: T; label: string }>;
  optionsPosition: 'left' | 'bottom';
  isDisabled?: boolean;
  dataTest?: string;
  getOptionsWidth?: (opts: {
    inputElement: HTMLElement | undefined;
  }) => number | undefined;
  renderInput(options: {
    ref: RefHandler;
    option?: { value: T; label: string };
    value: T;
    isOpened: boolean;
    isDisabled?: boolean;
    dataTest?: string;
    onToggle: () => void;
  }): React.ReactNode;
  onChange(value: T): void;
}

interface ILocalState {
  isOpened: boolean;
}

class Select<T> extends React.PureComponent<ILocalProps<T>, ILocalState> {
  public state: ILocalState = { isOpened: false };

  private inputRef: HTMLElement | undefined = undefined;

  public render() {
    const {
      rootClassname,
      value,
      renderInput,
      options,
      optionsPosition,
      getOptionsWidth,
      isDisabled,
      dataTest = 'select',
    } = this.props;
    const { isOpened } = this.state;

    return (
      <ClickOutsideListener onClickOutside={this.closeIfNeed}>
        <div
          className={cn(rootClassname, { [styles.disabled]: isDisabled })}
          data-test={`${dataTest}-root`}
          data-selected-value={value}
        >
          <PopperManager>
            <ReferenceForPopper>
              {({ ref }) =>
                renderInput({
                  ref: this.makeOnInputRef(ref),
                  option: options.find(option => option.value === value),
                  isOpened,
                  value,
                  isDisabled,
                  dataTest,
                  onToggle: this.onCurrentAccessClick,
                })
              }
            </ReferenceForPopper>
            <Popper placement={optionsPosition}>
              {({ ref, style }) => (
                <div
                  ref={ref}
                  className={cn(styles.options, { [styles.opened]: isOpened })}
                  style={{
                    ...style,
                    width:
                      getOptionsWidth &&
                      getOptionsWidth({ inputElement: this.inputRef }),
                  }}
                >
                  {options.map((option, i) => (
                    <div
                      className={cn(styles.option, {
                        [styles.option_selected]: option.value === value,
                      })}
                      key={i}
                      data-test={`${dataTest}-option`}
                      data-value={option.value}
                      onClick={this.makeOnChange(option.value)}
                    >
                      {option.label}
                    </div>
                  ))}
                </div>
              )}
            </Popper>
          </PopperManager>
        </div>
      </ClickOutsideListener>
    );
  }

  @bind
  private makeOnInputRef(poperRef: any): any {
    return (node: HTMLElement) => {
      poperRef(node);
      this.inputRef = node;
    };
  }

  @bind
  private onCurrentAccessClick() {
    if (this.props.isDisabled) {
      return;
    }
    if (this.state.isOpened) {
      this.close();
    } else {
      this.open();
    }
  }

  @bind
  private open() {
    this.setState({ isOpened: true });
  }
  @bind
  private close() {
    this.setState({ isOpened: false });
  }
  @bind
  private closeIfNeed() {
    if (this.state.isOpened) {
      this.close();
    }
  }

  @bind
  private makeOnChange(value: T) {
    return () => {
      if (this.props.value !== value) {
        this.props.onChange(value);
      }
      this.close();
    };
  }
}

export default Select;
