import { TextField } from '@material-ui/core';
import { TextFieldProps } from '@material-ui/core/TextField';
import * as React from 'react';
import { Omit } from 'react-redux';

import styles from './MuiTextInput.module.css';

class MuiTextInput extends React.PureComponent<
  TextFieldProps & { dataTest?: string }
> {
  public render() {
    const { dataTest, ...restProps } = this.props;
    return (
      <TextField
        {...restProps}
        inputProps={{
          'data-test': dataTest,
        }}
        className={styles.root}
        margin="none"
        fullWidth={true}
        variant="outlined"
      />
    );
  }
}

export default MuiTextInput;
