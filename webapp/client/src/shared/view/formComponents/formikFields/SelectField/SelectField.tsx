import { FieldConfig, Field } from 'formik';
import * as React from 'react';

import Select, {
  IOptionType,
} from 'shared/view/elements/Selects/Select/Select';

type AllProps<T> = FieldConfig &
  Omit<React.ComponentProps<typeof Select>, 'value'> & {
    options: Array<IOptionType<T>>;
    isMenuWithDynamicWidth?: boolean;
  };

export default class SelectField<T> extends React.Component<AllProps<T>> {
  public render() {
    return (
      <Field {...this.props}>
        {({ field, form }: any) => (
          <Select
            value={this.props.options.find(
              ({ value }) => value === field.value
            )}
            isCreatable={false}
            isMenuWithDynamicWidth={this.props.isMenuWithDynamicWidth}
            onChange={option => {
              form.setFieldValue(this.props.name, option.value);
            }}
            {...this.props}
          />
        )}
      </Field>
    );
  }
}
