import * as React from 'react';
import * as ReactDOM from 'react-dom';

import usePlacerUnderHeader from 'core/shared/view/pages/usePlacerUnderHeader';

import {
  IRequiredEntityInfo,
  IWithAddCommentFormSettings,
  IWithCommentSettings,
} from '../types';
import Comments from './Comments/Comments';
import styles from './OverflowingComments.module.css';

interface ILocalProps
  extends IWithAddCommentFormSettings,
    IWithCommentSettings {
  entityInfo: IRequiredEntityInfo;
  onClose(): void;
}

type AllProps = ILocalProps;

const appElement = document.getElementById('root')!;

const OverflowingComments = React.memo((props: AllProps) => {
  const {
    entityInfo,
    addCommentFormSettings,
    commentSettings,
    onClose,
  } = props;
  const { height, horizontalScrollOffset } = usePlacerUnderHeader({
    position: 'right',
  });

  return ReactDOM.createPortal(
    <div
      className={styles.root}
      style={{ height, right: horizontalScrollOffset }}
    >
      <div className={styles.overlay} onClick={onClose} />
      <div className={styles.comments}>
        <Comments
          entityInfo={entityInfo}
          addCommentFormSettings={addCommentFormSettings}
          commentSettings={commentSettings}
          onClose={onClose}
        />
      </div>
    </div>,
    appElement
  );
});

export type IOverflowingCommentsLocalProps = ILocalProps;
export default OverflowingComments;
