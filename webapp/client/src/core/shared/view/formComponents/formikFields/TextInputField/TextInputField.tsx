import TextInput from 'core/shared/view/elements/TextInput/TextInput';
import { FieldConfig, Field } from 'formik';
import * as React from 'react';

export default class TextInputField extends React.Component<
  FieldConfig &
    Omit<
      React.ComponentProps<typeof TextInput>,
      'value' | 'onChange' | 'onBlur' | 'onChangeWithEvent' | 'name'
    >
> {
  public render() {
    return (
      <Field {...this.props}>
        {({ field }: any) => (
          <TextInput
            value={field.value}
            name={field.name}
            onChangeWithEvent={field.onChange}
            onBlur={field.onBlur}
            {...this.props}
          />
        )}
      </Field>
    );
  }
}
