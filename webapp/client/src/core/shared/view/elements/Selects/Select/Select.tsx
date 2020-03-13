import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { FirstArgument } from 'core/shared/utils/types';

import BaseSelect from '../BaseSelect/BaseSelect';
import styles from './Select.module.css';

type ILocalProps<T> = Omit<
  React.ComponentProps<typeof BaseSelect>,
  'optionsPosition' | 'getOptionsWidth' | 'renderInput' | 'value' | 'onChange'
> & {
  value: T;
  onChange(value: T): void;
};

class Select<T> extends React.PureComponent<ILocalProps<T>> {
  public render() {
    return (
      <BaseSelect
        renderInput={this.renderInput}
        value={this.props.value}
        options={this.props.options}
        optionsPosition="bottom"
        getOptionsWidth={this.getOptionsWidth}
        dataTest="team-collaborators-select"
        isDisabled={this.props.isDisabled}
        onChange={this.props.onChange}
      />
    );
  }

  @bind
  private renderInput({
    ref,
    option,
    dataTest,
    isDisabled,
    onToggle,
  }: FirstArgument<React.ComponentProps<typeof BaseSelect>['renderInput']>) {
    return (
      <div
        className={cn(styles.input, { [styles.disabled]: isDisabled })}
        ref={ref}
        data-test={dataTest}
        onClick={onToggle}
      >
        {option ? option.label : ''}
      </div>
    );
  }

  @bind
  private getOptionsWidth({
    inputElement,
  }: FirstArgument<
    Required<React.ComponentProps<typeof BaseSelect>>['getOptionsWidth']
  >) {
    return inputElement ? inputElement.clientWidth : undefined;
  }
}

export default Select;
