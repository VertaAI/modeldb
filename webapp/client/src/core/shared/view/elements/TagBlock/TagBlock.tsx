import * as React from 'react';

import { makeDefaultTagFilter } from 'core/features/filter/Model';

import Draggable from '../Draggable/Draggable';
import styles from './TagBlock.module.css';

interface ILocalProps {
  tags: string[];
  isDraggable?: boolean;
}

export default class TagBlock extends React.Component<ILocalProps> {
  public render() {
    const { tags, isDraggable } = this.props;
    return (
      <ul className={styles.tags}>
        {tags.map((tag: string, i: number) => {
          return (
            <li key={i}>
              {isDraggable ? (
                <Draggable
                  type="Filter"
                  data={makeDefaultTagFilter(tag)}
                  additionalClassName={styles.projectTags}
                >
                  <span
                    className={styles.tag_draggable}
                    draggable={true}
                    title={tag}
                  >
                    {tag.length > 24 ? tag.substr(0, 23) + '..' : tag}
                  </span>
                </Draggable>
              ) : (
                <span className={styles.tag_static}>{tag}</span>
              )}
            </li>
          );
        })}
      </ul>
    );
  }
}
