import { InputAdornment } from '@material-ui/core';
import { FieldConfig, Field } from 'formik';
import * as React from 'react';

import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import MuiTextInput from 'core/shared/view/elements/MuiTextInput/MuiTextInput';

import styles from './MuiTextInputField.module.css';

type AllProps = FieldConfig &
  Omit<
    React.ComponentProps<typeof MuiTextInput>,
    'value' | 'onChange' | 'onBlur' | 'name' | 'error'
  > & {
    hint?: React.ReactNode;
    dataTestError?: string;
    isClerableInput?: boolean;
  };

export default class MuiTextInputField extends React.Component<AllProps> {
  public render() {
    return (
      <Field {...this.props}>
        {({ field, meta, form }: any) => {
          const isShowError = meta.touched && meta.error;
          return (
            <div>
              <div style={{ position: 'relative' }}>
                <MuiTextInput
                  multiline={this.props.multiline}
                  size={this.props.size}
                  rows={this.props.rows}
                  value={field.value}
                  name={field.name}
                  disabled={this.props.disabled}
                  label={this.props.label}
                  dataTest={this.props.dataTest}
                  error={Boolean(this.props.isClerableInput && isShowError)}
                  InputProps={
                    (this.props.isClerableInput &&
                      form.initialValues[this.props.name] !== field.value) ||
                    isShowError
                      ? {
                          endAdornment: (
                            <ClearInput
                              resetField={form.resetForm}
                              validateField={form.validateForm}
                            />
                          ),
                        }
                      : undefined
                  }
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                />
              </div>
              {isShowError ? (
                <InlineErrorView
                  error={meta.error}
                  dataTest={this.props.dataTestError}
                />
              ) : (
                this.props.hint || null
              )}
            </div>
          );
        }}
      </Field>
    );
  }
}

const ClearInput = ({
  resetField,
  validateField,
}: {
  resetField: () => void;
  validateField: () => void;
}) => {
  const onClick = () => {
    resetField();
    validateField();
  };

  return (
    <InputAdornment position="end">
      <div className={styles.clearInput} onClick={onClick}>
        x
      </div>
    </InputAdornment>
  );
};
