import * as React from 'react';

import { PropertyType } from 'models/Filters';

import Draggable from '../Draggable/Draggable';
import styles from './TagBlock.module.css';

interface ILocalProps {
  tag: string;
}

export default class Tags extends React.Component<ILocalProps> {
  public render() {
    const { tag } = this.props;
    return (
      <Draggable
        type="Filter"
        data={{ type: PropertyType.STRING, name: 'Tag', value: tag }}
        additionalClassName={styles.tag}
      >
        <span draggable={true} title={`Drag & Drop To Filter`}>
          {tag}
        </span>
      </Draggable>
    );
  }
}
