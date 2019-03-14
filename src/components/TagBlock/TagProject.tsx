import * as React from 'react';
import Draggable from '../Draggable/Draggable';
import { PropertyType } from '../../models/Filters';
import styles from './TagBlock.module.css';

interface ILocalProps {
  tag: string;
}

export default class Tags extends React.Component<ILocalProps> {
  public render() {
    const { tag } = this.props;
    return (
      <div className={styles.tag} style={{ border: 'none', marginLeft: '10px' }}>
        <Draggable type="Filter" data={{ type: PropertyType.STRING, name: 'Tag', value: tag }} additionalClassName={styles.projectTags}>
          <span draggable={true} title={`Drag & Drop To Filter`}>
            {tag}
          </span>
        </Draggable>
      </div>
    );
  }
}
