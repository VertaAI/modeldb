import * as React from 'react';

function makeWrapperComponent(Wrapper: React.ComponentType<any>) {
  return function<Props>(Component: React.ComponentType<Props>) {
    return function(props: Props) {
      return (
        <Wrapper>
          <Component {...props} />
        </Wrapper>
      );
    };
  };
}

export default makeWrapperComponent;
