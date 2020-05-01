import { TextField } from '@material-ui/core';
import { TextFieldProps } from '@material-ui/core/TextField';
import cn from 'classnames';
import * as React from 'react';

import styles from './MuiTextInput.module.css';

class MuiTextInput extends React.PureComponent<
  Omit<TextFieldProps, 'size'> & {
    dataTest?: string;
    size?: 'small' | 'extraSmall';
    isError?: boolean;
  }
> {
  public render() {
    const { dataTest, size, isError, ...restProps } = this.props;
    return (
      <TextField
        {...restProps}
        inputProps={{
          'data-test': dataTest,
        }}
        error={this.props.error}
        className={cn(styles.root, {
          [styles.size_small]: size === 'small',
          [styles.size_extraSmall]: size === 'extraSmall',
          [styles.error]: isError,
        })}
        margin="none"
        fullWidth={true}
        variant="outlined"
      />
    );
  }
}

export default MuiTextInput;