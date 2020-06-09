import { shallow, mount, ShallowWrapper, ReactWrapper } from 'enzyme';
import * as R from 'ramda';
import React from 'react';
import { act } from 'react-dom/test-utils';
import { flushAllPromisesFor } from '../integrations/flushAllPromisesFor';

export function makeShallowRenderer<Props = {}, State = {}>(
  Component: React.ComponentClass<Props, State>,
  defaultProps: Props
) {
  return (props: Partial<Props> = {}): ShallowWrapper<Props, State> =>
    shallow(<Component {...defaultProps} {...props} />);
}

export function makeMountRenderer<Props = {}, State = {}>(
  Component: React.ComponentClass<Props, State>,
  defaultProps: Props
) {
  return (props: Partial<Props> = {}): ReactWrapper<Props, State> =>
    mount(<Component {...defaultProps} {...props} />);
}

export const findByText = (
  text: string,
  wrapper: ShallowWrapper<any, any> | ReactWrapper<any, any>
) => {
  return wrapper
    .findWhere((n: any) => {
      return n.text() === text;
    })
    .last();
};

export const findByDataTestAttribute = R.curry(
  (
    text: string,
    wrapper: ShallowWrapper<any, any> | ReactWrapper<any, any>
  ): ReactWrapper<any, any> => {
    return wrapper.findWhere((n: any) => {
      return n.prop('data-test') === text;
    }) as any;
  }
);

export const makeInputHelpers = (
  getInput: (component: ReactWrapper) => ReactWrapper
) => {
  const change = (value: string, component: ReactWrapper) => {
    getInput(component).simulate('change', { target: { value } });
    component.update();
  };
  const blur = (component: ReactWrapper) => {
    getInput(component).simulate('blur');
    component.update();
  };
  const changeAndBlur = (value: string, component: ReactWrapper) => {
    change(value, component);
    blur(component);
  };
  const getValue = (component: ReactWrapper) =>
    getInput(component).prop('value');

  return { change, blur, changeAndBlur, getValue };
};

export const makeAsyncInputHelpersByName = (name: string) => {
  const getInput = (component: ReactWrapper) => {
    const inputNode = component.find(`input[name="${name}"]`);
    if (inputNode.length === 0) {
      return component.find(`textarea[name="${name}"]`);
    }
    return inputNode;
  };
  const change = async (value: string, component: ReactWrapper) => {
    await act(async () => {
      getInput(component).simulate('change', { target: { name, value } });
    });
    component.update();
  };
  const blur = async (component: ReactWrapper<any, any>) => {
    await act(async () => {
      getInput(component).simulate('blur', { target: { name } });
    });
    component.update();
  };
  const changeAndBlur = async (value: string, component: ReactWrapper) => {
    await change(value, component);
    await blur(component);
  };
  const getValue = (component: ReactWrapper) =>
    getInput(component).prop('value');

  return { change, blur, changeAndBlur, getValue, getInput };
};

export const submitAsyncForm = async (
  nativeFormWrapper: ReactWrapper<any, any, React.Component<{}, {}, any>>,
  component: ReactWrapper
) => {
  await act(async () => {
    nativeFormWrapper.simulate('submit', { preventDefault: () => {} });
    if (component) {
      await flushAllPromisesFor(component);
    }
  });
};

export const makeAsyncCheckboxHelpersByName = (name: string) => {
  const getInput = (component: ReactWrapper) =>
    component.find(`input[name="${name}"]`);
  const change = async (value: boolean, component: ReactWrapper) => {
    await act(async () => {
      getInput(component).simulate('change', { target: { name, value } });
    });
    component.update();
  };
  const getValue = (component: ReactWrapper) =>
    getInput(component).prop('checked');

  return { change, getValue, getInput };
};

export async function withAct<T>(f: () => T): Promise<T> {
  let result: any;
  await act(async () => {
    result = await f();
  });
  return result as T;
}
