import { bind } from 'decko';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

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

class OverflowingComments extends React.PureComponent<AllProps> {
  public render() {
    const { entityInfo, addCommentFormSettings, commentSettings } = this.props;
    return ReactDOM.createPortal(
      <div className={styles.root}>
        <div className={styles.overlay} onClick={this.onClose} />
        <div className={styles.comments}>
          <Comments
            entityInfo={entityInfo}
            addCommentFormSettings={addCommentFormSettings}
            commentSettings={commentSettings}
            onClose={this.onClose}
          />
        </div>
      </div>,
      appElement
    );
  }

  @bind
  private onClose() {
    this.props.onClose();
  }
}

export type IOverflowingCommentsLocalProps = ILocalProps;
export default OverflowingComments;
