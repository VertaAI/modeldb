import { Radio } from '@material-ui/core';
import * as React from 'react';

import styles from './RadioButton.module.css';

interface ILocalProps {
  value: boolean;
  name?: string;
  onChange(e: React.ChangeEvent<HTMLInputElement>): void;
}

const RadioButton = ({ value, name, onChange }: ILocalProps) => {
  return (
    <Radio
      className={styles.root}
      checked={value}
      color="primary"
      disableRipple={true}
      name={name}
      onChange={onChange}
    />
  );
};

export default RadioButton;
