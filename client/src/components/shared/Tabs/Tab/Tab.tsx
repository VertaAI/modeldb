import * as React from 'react';

export interface ILocalProps<T> {
  title: string;
  type: T;
  children: any;
  badge?: number;
  centered?: boolean; // todo rename
}

class Tab<T> extends React.Component<ILocalProps<T>> {
  public render() {
    return this.props.children;
  }
}

export default Tab;
