import * as React from 'react';

export interface IProps<T> {
  title: string;
  type: T;
  children: React.ReactChild | React.ReactChildren;
  badge?: number;
}

class Tab<T> extends React.Component<IProps<T>> {
  public render() {
    return this.props.children;
  }
}

export default Tab;
