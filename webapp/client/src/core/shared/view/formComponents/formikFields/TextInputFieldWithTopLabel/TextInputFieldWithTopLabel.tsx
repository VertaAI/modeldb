import { FieldConfig, Field } from 'formik';
import * as React from 'react';

import FieldWithTopLabel from 'core/shared/view/elements/FieldWithTopLabel/FieldWithTopLabel';
import TextInput from 'core/shared/view/elements/TextInput/TextInput';

export default class TextInputFieldWithTopLabel extends React.Component<
  FieldConfig &
    Omit<
      React.ComponentProps<typeof TextInput>,
      'value' | 'onChange' | 'onBlur' | 'onChangeWithEvent' | 'name'
    > & {
      label: string;
      isRequired?: boolean;
    }
> {
  public render() {
    return (
      <Field {...this.props}>
        {({ field, meta }: any) => (
          <FieldWithTopLabel
            label={this.props.label}
            isRequired={this.props.isRequired}
            meta={meta}
          >
            <TextInput
              value={field.value}
              name={field.name}
              onChangeWithEvent={field.onChange}
              onBlur={field.onBlur}
              {...this.props}
            />
          </FieldWithTopLabel>
        )}
      </Field>
    );
  }
}
