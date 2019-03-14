import * as React from 'react';
import styles from './Brush.module.css';

interface ILocalProps {
  brush: string;
}

export default class Brush extends React.Component<ILocalProps> {
  public render() {
    const { brush } = this.props;
    return <span />;
  }
}
