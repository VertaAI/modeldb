import cn from 'classnames';
import * as React from 'react';

import {
  IStringFilterData,
  IExperimentNameFilterData,
  StringFilterOperator,
  OperatorType,
} from 'core/shared/models/Filters';
import TextInput from 'core/shared/view/elements/TextInput/TextInput';

import FilterSelect from '../FilterSelect/FilterSelect';
import styles from './StringFilterEditor.module.css';

interface ILocalProps {
  data: IStringFilterData | IExperimentNameFilterData;
  onChange: (newData: IStringFilterData | IExperimentNameFilterData) => void;
  isReadonly?: boolean;
}

const comparisonOptions = (() => {
  const map: {
    [T in Exclude<StringFilterOperator, 'NOT_LIKE'>]: {
      value: T;
      label: string;
    }
  } = {
    [OperatorType.EQUALS]: {
      value: OperatorType.EQUALS,
      label: '=',
    },
    [OperatorType.NOT_EQUALS]: {
      value: OperatorType.NOT_EQUALS,
      label: '!=',
    },
    [OperatorType.LIKE]: {
      value: OperatorType.LIKE,
      label: 'LIKE',
    },
  };
  return Object.values(map);
})();

const StringFilterEditor: React.FC<ILocalProps> = ({
  data,
  isReadonly,
  onChange,
}) => {
  const [newValue, changeNewValue] = React.useState(data.value);

  React.useEffect(() => {
    changeNewValue(data.value);
  }, [data.value]);

  const currentOption = comparisonOptions.find(o => o.value === data.operator);

  const onSave = React.useCallback(
    (filterData: IStringFilterData | IExperimentNameFilterData) => {
      onChange(filterData);
    },
    [onChange]
  );

  const onChangeOperator = React.useCallback(
    ({ value }: { value: StringFilterOperator }) => {
      onSave({
        ...data,
        operator: value as any,
      });
    },
    [onSave, data]
  );

  const onSubmit = React.useCallback(
    (event: React.KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'Enter' && data.value !== newValue) {
        const newData = { ...data, value: newValue };
        onSave(newData);
      }
    },
    [onSave, data, newValue]
  );

  const onBlur = React.useCallback(() => {
    if (newValue !== data.value) {
      const newData = { ...data, value: newValue };
      onSave(newData);
    }
  }, [onSave, data, newValue]);

  const canculateInputWidthByText = ({
    paddingInPx,
    string,
  }: {
    string: string;
    paddingInPx: number;
  }) => {
    const width = string.length > 9 ? string.length * 7 : 80;
    return `${width + paddingInPx}px`;
  };

  return (
    <div
      className={cn(styles.root, {
        [styles.readonly]: isReadonly,
      })}
    >
      <div className={styles.operator}>
        <FilterSelect
          currentOption={currentOption}
          onChange={onChangeOperator}
          options={comparisonOptions}
          isReadonly={isReadonly}
        />
      </div>
      <div
        className={styles.input}
        style={{
          width: canculateInputWidthByText({
            string: newValue,
            paddingInPx: 24,
          }),
        }}
      >
        <TextInput
          value={newValue}
          onChange={changeNewValue}
          size="small"
          dataTest="filter-item-value"
          onKeyUp={onSubmit}
          onBlur={onBlur}
          isDisabled={isReadonly}
          fullWidth={true}
        />
      </div>
    </div>
  );
};

export default React.memo(StringFilterEditor);
