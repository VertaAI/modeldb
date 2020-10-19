import * as React from 'react';
import DatePickerLibrary from 'react-datepicker';

import 'react-datepicker/dist/react-datepicker.css';
import styles from './DatePicker.module.css';

interface ILocalProps {
  value: Date | undefined;
  minDate?: Date;
  maxDate?: Date;
  isShowTimeSelect?: boolean;
  disabled?: boolean;
  onChange(value: Date): void;
  onBlur?(): void;
  onKeyDown?(event: React.KeyboardEvent<HTMLDivElement>): void;
}

class DatePicker extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      value,
      minDate,
      maxDate,
      isShowTimeSelect,
      disabled,
      onChange,
      onBlur,
      onKeyDown,
    } = this.props;

    return (
      <div className={styles.root}>
        <DatePickerLibrary
          selected={value}
          minDate={minDate}
          maxDate={maxDate}
          showTimeSelect={isShowTimeSelect}
          timeFormat="HH:mm"
          timeIntervals={15}
          timeCaption="time"
          disabled={disabled}
          disabledKeyboardNavigation={true}
          dateFormat="MM/dd/yyyy hh:mm:ss"
          onChange={onChange}
          onCalendarClose={() => {
            if (onBlur) {
              onBlur();
            }
          }}
          onBlur={onBlur}
          onKeyDown={onKeyDown}
        />
      </div>
    );
  }
}

export default DatePicker;
