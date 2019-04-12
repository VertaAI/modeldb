import * as React from 'react';

import { PropertyType } from 'models/Filters';

import Draggable from '../Draggable/Draggable';
import Tag from './Tag';
import styles from './TagBlock.module.css';

interface ILocalProps {
  tags: string[];
}

export default class Tags extends React.Component<ILocalProps> {
  public render() {
    const { tags } = this.props;
    return (
      <ul className={styles.tags}>
        {tags.map((tag: string, i: number) => {
          return (
            <li key={i}>
              <Draggable
                type="Filter"
                data={{ type: PropertyType.STRING, name: 'Tag', value: tag }}
              >
                <Tag tag={tag} />
              </Draggable>
            </li>
          );
        })}
      </ul>
    );
  }
}
