import * as React from 'react';
import { Omit } from 'react-redux';

function withProps<P>(Child: React.ComponentType<P>) {
  return <F extends Partial<P>>(props: F) => (
    restProps: Omit<P, Extract<keyof F, keyof P>>
  ) => <Child {...props} {...restProps as any} />;
}

export default withProps;
