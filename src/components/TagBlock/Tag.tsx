import * as React from 'react';
import styles from './TagBlock.module.css';

interface ILocalProps {
  tag: string;
}

export default class Tags extends React.Component<ILocalProps> {
  public render() {
    const { tag } = this.props;
    return (
      <a href="#" className={styles.tag}>
        {tag}
      </a>
    );
  }
}
