import cn from 'classnames';
import React, { useCallback, useState, useRef, useEffect } from 'react';

import { Icon } from 'shared/view/elements/Icon/Icon';

import styles from './FilterSelect.module.css';

export interface IFilterOption<T> {
  label: string;
  value: T;
}

interface ILocalProps<T> {
  options: Array<IFilterOption<T>>;
  currentOption: IFilterOption<T> | undefined;
  isReadonly?: boolean;
  onChange(option: IFilterOption<T>): void;
}

const FilterSelect = <T extends any = string>({
  currentOption,
  onChange,
  options,
  isReadonly,
}: ILocalProps<T>) => {
  const [isOpen, changeOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    if (isReadonly) {
      onClose();
    }
  }, [isReadonly]);

  const onToggle = useCallback(() => {
    if (!isReadonly) {
      changeOpen(!isOpen);
    }
  }, [isOpen]);

  const onClose = useCallback(() => changeOpen(false), []);

  return (
    <div
      className={cn(styles.root, {
        [styles.open]: isOpen,
        [styles.readonly]: isReadonly,
      })}
      data-test="filter-select"
      data-disabled={Boolean(isReadonly)}
      ref={rootRef}
      onClick={onToggle}
    >
      <div className={styles.current} data-test="filter-select-value">
        {currentOption && currentOption.label}
      </div>
      <Icon type="arrow-down" className={styles.icon} />
      {isOpen && (
        <div className={styles.optionsList} data-test="filter-select-options">
          {options.map(opt => (
            <FilterSelectOption
              key={opt.label}
              option={opt}
              isActive={
                currentOption !== undefined && currentOption.value === opt.value
              }
              onChange={onChange}
            />
          ))}
        </div>
      )}
    </div>
  );
};

const FilterSelectOption = <T extends any>({
  option,
  isActive,
  onChange,
}: {
  option: IFilterOption<T>;
  isActive: boolean;
  onChange(option: IFilterOption<T>): void;
}) => {
  const onClick = useCallback(() => onChange(option), [option, onChange]);

  return (
    <div
      className={cn(styles.option, { [styles.option_active]: isActive })}
      data-test={`quick-filter-item-${option.label}`}
      data-target={`quick-filter-item`}
      onClick={onClick}
    >
      {option.label}
    </div>
  );
};

export default React.memo(FilterSelect);
