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
          dateFormat="MM/dd/yyyy h:mm aa"
          onChange={onChange}
        />
      </div>
    );
  }
}

export default DatePicker;
