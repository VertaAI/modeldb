import { Formik, FormikConfig, Form } from 'formik';
import * as React from 'react';

export default class PresetFormik<Values> extends React.Component<
  Omit<FormikConfig<Values>, 'validateOnBlur'>
> {
  public render() {
    return (
      <Formik
        {...this.props}
        validateOnMount={
          typeof this.props.validateOnMount === 'undefined'
            ? true
            : this.props.validateOnMount
        }
        validateOnBlur={true}
      >
        {childrenProps => (
          <Form>{(this.props.children as any)(childrenProps)}</Form>
        )}
      </Formik>
    );
  }
}
