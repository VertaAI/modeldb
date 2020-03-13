import * as React from 'react';

import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import styles from './Collapsable.module.css';

interface ILocalProps {
  collapseLabel?: string;
  collapsibleContainerID: string;
  children?: React.ReactChild | React.ReactChildren;
  keepOpen?: boolean;
}

class Collapsable extends React.Component<ILocalProps> {
  public render() {
    const {
      collapseLabel,
      children,
      keepOpen,
      collapsibleContainerID,
    } = this.props;
    return (
      <div className={styles.wrap_collabsible}>
        <input
          id={collapsibleContainerID}
          className={styles.toggle}
          type="checkbox"
        />
        <label
          htmlFor={collapsibleContainerID}
          className={keepOpen ? styles.lbl_toggle_open : styles.lbl_toggle}
        >
          {collapseLabel && (
            <span className={styles.label_content}>{collapseLabel}</span>
          )}
        </label>
        <div
          className={
            keepOpen
              ? styles.collapsible_content_open
              : styles.collapsible_content
          }
        >
          <div className={styles.content_inner}>
            <ScrollableContainer
              children={children}
              maxHeight={240}
              containerOffsetValue={20}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default Collapsable;
