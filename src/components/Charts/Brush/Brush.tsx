import * as React from 'react';
import styles from './Brush.module.css';

interface ILocalProps {
  tag: string;
}

export default class Brush extends React.Component<ILocalProps> {
  public render() {
    const { tag } = this.props;
    return <span />;
  }
}
