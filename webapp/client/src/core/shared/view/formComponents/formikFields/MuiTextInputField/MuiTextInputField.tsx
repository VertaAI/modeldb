import { FieldConfig, Field } from 'formik';
import * as React from 'react';

import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import MuiTextInput from 'core/shared/view/elements/MuiTextInput/MuiTextInput';

type AllProps = FieldConfig &
  Omit<
    React.ComponentProps<typeof MuiTextInput>,
    'value' | 'onChange' | 'onBlur' | 'name'
  > & { hint?: React.ReactNode; dataTestError?: string };

export default class MuiTextInputField extends React.Component<AllProps> {
  public render() {
    return (
      <Field {...this.props}>
        {({ field, meta }: any) => (
          <div>
            <MuiTextInput
              multiline={this.props.multiline}
              rows={this.props.rows}
              value={field.value}
              name={field.name}
              disabled={this.props.disabled}
              label={this.props.label}
              dataTest={this.props.dataTest}
              onChange={field.onChange}
              onBlur={field.onBlur}
            />
            {meta.touched && meta.error ? (
              <InlineErrorView
                error={meta.error}
                dataTest={this.props.dataTestError}
              />
            ) : (
              this.props.hint
            )}
          </div>
        )}
      </Field>
    );
  }
}
