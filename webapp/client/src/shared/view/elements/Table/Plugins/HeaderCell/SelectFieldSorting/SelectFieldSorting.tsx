import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { ISorting } from 'shared/models/Sorting';
import ClickOutsideListener from 'shared/view/elements/ClickOutsideListener/ClickOutsideListener';
import { Icon } from 'shared/view/elements/Icon/Icon';

import styles from './SelectFieldSorting.module.css';

interface IProps {
  fields: IField[];
  selected: ISorting | null;
  columnName: string;
  onChange(sorting: ISorting | null): void;
}

export interface IField {
  label: string;
  name: string;
}

interface ILocalState {
  isOpen: boolean;
}

class SelectFieldSorting extends React.PureComponent<IProps, ILocalState> {
  public state: ILocalState = {
    isOpen: false,
  };

  public render() {
    const { fields, selected } = this.props;
    const { isOpen } = this.state;
    return (
      <ClickOutsideListener onClickOutside={this.close}>
        <div
          className={cn(styles.root, { [styles.selected]: Boolean(selected) })}
        >
          <div
            className={cn(
              styles.field_sorting_indicator,
              styles.field_sorting_indicator_top
            )}
            data-test="select-field-sorting"
            onClick={this.open}
          >
            <Icon
              type="arrow-up-lite"
              className={cn(styles.header_sorting_indicator_asc, {
                [styles.selected]: selected && selected.direction === 'asc',
              })}
            />
            <Icon
              type="arrow-down-lite"
              className={cn(styles.header_sorting_indicator_desc, {
                [styles.selected]: selected && selected.direction === 'desc',
              })}
            />
          </div>
          {isOpen && (
            <div className={styles.options}>
              {fields.map(field => {
                const isSelectedOption = selected
                  ? selected.fieldName === field.name
                  : false;

                return (
                  <div
                    className={cn(styles.option)}
                    key={field.name}
                    data-test="select-field-sorting-item"
                  >
                    <span className={styles.option_label}>{field.label}</span>
                    <div className={styles.option_sort_direction_buttons}>
                      <button
                        className={cn(styles.option_sort_direction_button, {
                          [styles.selected]:
                            isSelectedOption && selected!.direction === 'asc',
                        })}
                        data-test="select-field-item-asc-direction"
                        onClick={this.makeOnChangeSorting(field.name, 'asc')}
                      >
                        <Icon type="arrow-up-solid" />
                      </button>
                      <button
                        className={cn(styles.option_sort_direction_button, {
                          [styles.selected]:
                            isSelectedOption && selected!.direction === 'desc',
                        })}
                        data-test="select-field-item-desc-direction"
                        onClick={this.makeOnChangeSorting(field.name, 'desc')}
                      >
                        <Icon type="arrow-down-solid" />
                      </button>
                      <Icon
                        type="cancel"
                        className={cn(styles.reset_sorting, {
                          [styles.hidden]: !isSelectedOption,
                        })}
                        onClick={this.onResetSorting}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </ClickOutsideListener>
    );
  }

  @bind
  private open() {
    this.setState({ isOpen: true });
  }

  @bind
  private close() {
    this.setState({ isOpen: false });
  }

  @bind
  private onResetSorting(e: React.MouseEvent<HTMLDivElement>) {
    e.stopPropagation();
    if (this.props.selected) {
      this.props.onChange(null);
    }
  }

  @bind
  private makeOnChangeSorting(
    fieldName: string,
    direction: ISorting['direction']
  ) {
    return () => {
      this.props.onChange({
        direction,
        fieldName,
        columnName: this.props.columnName,
      });
    };
  }
}

export default SelectFieldSorting;
