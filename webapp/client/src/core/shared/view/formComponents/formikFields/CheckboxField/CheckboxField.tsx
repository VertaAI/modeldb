import { FieldConfig, Field } from 'formik';
import * as React from 'react';

import Checkbox from 'core/shared/view/elements/Checkbox/Checkbox';

export default class CheckboxField extends React.Component<
  FieldConfig &
    Omit<
      React.ComponentProps<typeof Checkbox>,
      'value' | 'onBlur' | 'onChangeWithEvent' | 'name'
    >
> {
  public render() {
    return (
      <Field {...this.props}>
        {({ field, form }: any) => (
          <Checkbox
            value={field.value}
            name={field.name}
            {...this.props}
            onChange={value => {
              form.setFieldValue(this.props.name, value);
              if (this.props.onChange) {
                this.props.onChange(value);
              }
            }}
          />
        )}
      </Field>
    );
  }
}
