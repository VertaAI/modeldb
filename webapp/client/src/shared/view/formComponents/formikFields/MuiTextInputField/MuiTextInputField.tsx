import { FieldConfig, Field } from 'formik';
import * as React from 'react';

import InlineErrorView from 'shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import MuiTextInput from 'shared/view/elements/MuiTextInput/MuiTextInput';

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
                  resetValueControl={
                    (this.props.isClerableInput &&
                      form.initialValues[this.props.name] !== field.value) ||
                    isShowError
                      ? {
                          onReset: () => {
                            form.resetForm();
                            setTimeout(() => {
                              form.validateForm();
                            }, 0);
                          },
                        }
                      : undefined
                  }
                  placeholder={this.props.placeholder}
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
