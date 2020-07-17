import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import {
  OperatorType,
  IDateFilterData,
  NumberFilterOperator,
} from 'shared/models/Filters';
import DatePicker from 'shared/view/elements/DatePicker/DatePicker';

import styles from './DateFilterEditor.module.css';
import FilterSelect from '../FilterSelect/FilterSelect';

interface ILocalProps {
  data: IDateFilterData;
  onChange: (newData: IDateFilterData) => void;
  isReadonly?: boolean;
}

const operatorOptions = (() => {
  const map: { [T in NumberFilterOperator]: { value: T; label: string } } = {
    [OperatorType.MORE]: {
      value: OperatorType.MORE,
      label: '>',
    },
    [OperatorType.GREATER_OR_EQUALS]: {
      value: OperatorType.GREATER_OR_EQUALS,
      label: '>=',
    },
    [OperatorType.EQUALS]: {
      value: OperatorType.EQUALS,
      label: '=',
    },
    [OperatorType.NOT_EQUALS]: {
      value: OperatorType.NOT_EQUALS,
      label: '!=',
    },
    [OperatorType.LESS]: {
      value: OperatorType.LESS,
      label: '<',
    },
    [OperatorType.LESS_OR_EQUALS]: {
      value: OperatorType.LESS_OR_EQUALS,
      label: '<=',
    },
  };
  return Object.values(map);
})();

export default class DateFilterEditor extends React.Component<
  ILocalProps,
  { value: number | undefined }
> {
  public state: { value: number | undefined } = {
    value: this.props.data.value,
  };

  public render() {
    const { data, isReadonly } = this.props;

    return (
      <div
        className={cn(styles.root, {
          [styles.readonly]: isReadonly,
        })}
      >
        <div>
          <FilterSelect
            options={operatorOptions}
            currentOption={operatorOptions.find(o => o.value === data.operator)}
            onChange={({ value }) => this.onComparisonChanged(value)}
            isReadonly={isReadonly}
          />
        </div>

        <div className={styles.input}>
          <DatePicker
            value={this.state.value ? new Date(this.state.value) : undefined}
            onChange={value => {
              this.setState({
                value: +value,
              });
            }}
            onBlur={this.onBlur}
            onKeyDown={this.onSubmit}
          />
        </div>
      </div>
    );
  }

  @bind
  private onSave(data: IDateFilterData) {
    if (this.props.onChange) {
      this.props.onChange(data);
    }
  }

  @bind
  private onComparisonChanged(operator: NumberFilterOperator) {
    this.onSave({
      ...this.props.data,
      operator,
    });
  }

  @bind
  private onSubmit(event: React.KeyboardEvent<HTMLInputElement>) {
    if (this.state.value && event.key === 'Enter') {
      this.onSave({
        ...this.props.data,
        value: +this.state.value,
      });
    }
  }

  @bind
  private onBlur() {
    if (this.state.value) {
      this.onSave({ ...this.props.data, value: +this.state.value });
    }
  }
}
