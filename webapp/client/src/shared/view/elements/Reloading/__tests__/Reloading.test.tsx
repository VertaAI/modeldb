import { mount } from 'enzyme';
import React from 'react';
import { Route } from 'react-router';

import {
  findByDataTestAttribute,
  withAct,
} from 'shared/utils/tests/react/helpers';

import Reloading from '../Reloading';
import makeMountComponentWithPredefinedData from 'shared/utils/tests/integrations/makeMountComponentWithPredefinedData';

const makeComponent = async ({
  pathname,
  onReload,
  children,
}: {
  pathname: string;
  onReload: () => void;
  children: React.ReactNode;
}) => {
  const data = await makeMountComponentWithPredefinedData({
    Component: () => <Reloading onReload={onReload}>{children}</Reloading>,
    settings: {
      pathname,
    },
  });

  return data;
};

describe('(Reloading component)', () => {
  it('should render content', async () => {
    const { component } = await makeComponent({
      pathname: '/test',
      children: <div data-test="content">Content</div>,
      onReload: jest.fn(),
    });

    expect(findByDataTestAttribute('content', component).length).toEqual(1);

    expect(findByDataTestAttribute('content', component).text()).toEqual(
      'Content'
    );
  });

  it('should update children', async () => {
    const { component } = await makeComponent({
      pathname: '/test',
      children: <div data-test="content">Content</div>,
      onReload: jest.fn(),
    });

    expect(findByDataTestAttribute('content', component).text()).toEqual(
      'Content'
    );

    component.setProps({
      children: <div data-test="content">New content</div>,
    });

    expect(findByDataTestAttribute('content', component).text()).toEqual(
      'New content'
    );
  });

  it('should call onReload then was redirect on current route', async () => {
    const onReloadMock = jest.fn();
    const currentPathname = '/test';

    const { component, history } = await makeComponent({
      pathname: currentPathname,
      children: <div data-test="content">Content</div>,
      onReload: onReloadMock,
    });

    history.push(currentPathname);

    expect(onReloadMock).toHaveBeenCalledTimes(1);
  });

  it('shouldn`t call onReload then was redirect on different route', async () => {
    const onReloadMock = jest.fn();
    const currentPathname = '/test';
    const newRoute = '/test-new';

    const { component, history } = await makeComponent({
      pathname: currentPathname,
      children: <div data-test="content">Content</div>,
      onReload: onReloadMock,
    });

    history.push(newRoute);

    expect(onReloadMock).toHaveBeenCalledTimes(0);
  });
});
