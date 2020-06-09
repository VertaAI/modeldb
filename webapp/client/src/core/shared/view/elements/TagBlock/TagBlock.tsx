import * as React from 'react';
import cn from 'classnames';

import { makeDefaultTagFilter } from 'core/shared/models/Filters';

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
        {tags.map((tag, i) => {
          return (
            <li key={i}>
              {isDraggable ? (
                <Draggable
                  type="Filter"
                  data={makeDefaultTagFilter(tag)}
                  additionalClassName={styles.projectTags}
                >
                  <span
                    className={cn(styles.tag, { [styles.draggable]: true })}
                    draggable={true}
                    title={tag}
                  >
                    {tag.length > 24 ? tag.substr(0, 23) + '..' : tag}
                  </span>
                </Draggable>
              ) : (
                <span className={styles.tag}>{tag}</span>
              )}
            </li>
          );
        })}
      </ul>
    );
  }
}
