import { TextField, InputAdornment } from '@material-ui/core';
import { TextFieldProps } from '@material-ui/core/TextField';
import cn from 'classnames';
import * as React from 'react';

import styles from './MuiTextInput.module.css';
import { Icon } from '../Icon/Icon';

type ILocalProps = Omit<TextFieldProps, 'size'> & {
  dataTest?: string;
  size?: 'small' | 'extraSmall';
  isError?: boolean;
  resetValueControl?: {
    onReset: () => void;
  };
};

const MuiTextInput = React.forwardRef<HTMLInputElement, ILocalProps>(
  (
    { dataTest, size, error, isError, resetValueControl, ...restProps },
    ref
  ) => {
    return (
      <TextField
        {...restProps}
        ref={ref as any}
        inputProps={{
          ...restProps.inputProps,
          'data-test': dataTest,
        }}
        error={error}
        className={cn(styles.root, {
          [styles.size_small]: size === 'small',
          [styles.size_extraSmall]: size === 'extraSmall',
          [styles.error]: isError,
        })}
        InputProps={
          resetValueControl
            ? {
                ...(restProps || {}).InputProps,
                endAdornment: <ResetValueControl {...resetValueControl} />,
              }
            : restProps.InputProps
        }
        margin="none"
        fullWidth={true}
        variant="outlined"
      />
    );
  }
);

const ResetValueControl = ({ onReset }: { onReset(): void }) => {
  return (
    <InputAdornment position="end">
      <div className={styles.resetValueControl} onClick={onReset}>
        <Icon type="close" />
      </div>
    </InputAdornment>
  );
};

export default MuiTextInput;
