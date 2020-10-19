import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { OperatorType, IMetricFilterData } from 'shared/models/Filters';
import { withScientificNotationOrRounded } from 'shared/utils/formatters/number';
import TextInput from 'shared/view/elements/TextInput/TextInput';

import styles from './MetricFilterEditor.module.css';
import FilterSelect from '../FilterSelect/FilterSelect';

interface ILocalProps {
  data: IMetricFilterData;
  onChange: (newData: IMetricFilterData) => void;
  isReadonly?: boolean;
}

const operatorOptions = (() => {
  const map: {
    [T in IMetricFilterData['operator']]: { value: T; label: string };
  } = {
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

export default class MetricFilterEditor extends React.Component<ILocalProps> {
  public render() {
    const { data, isReadonly } = this.props;

    const defaultValue = withScientificNotationOrRounded(
      this.props.data.value
    ).toString();

    const canculateInputWidthByText = ({
      paddingInPx,
      string,
    }: {
      string: string;
      paddingInPx: number;
    }) => {
      const width = string.length > 9 ? string.length * 8 : 80;
      return `${width + paddingInPx}px`;
    };

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

        <div
          className={styles.input}
          style={{
            width: canculateInputWidthByText({
              string: defaultValue,
              paddingInPx: 24,
            }),
          }}
        >
          <TextInput
            size="small"
            defaultValue={defaultValue}
            dataTest="filter-item-value"
            onBlur={this.onBlur}
            onKeyUp={this.onSubmit}
            isDisabled={isReadonly}
            fullWidth={true}
          />
        </div>
      </div>
    );
  }

  @bind
  private onSave(data: IMetricFilterData) {
    if (this.props.onChange) {
      this.props.onChange(data);
    }
  }

  @bind
  private onComparisonChanged(operator: IMetricFilterData['operator']) {
    this.onSave({
      ...this.props.data,
      operator,
    });
  }

  @bind
  private onSubmit(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      this.onSave({
        ...this.props.data,
        value: Number(event.currentTarget.value),
      });
    }
  }

  @bind
  private onBlur(event: React.ChangeEvent<HTMLInputElement>) {
    this.onSave({ ...this.props.data, value: Number(event.target.value) });
  }
}
