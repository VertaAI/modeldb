import * as React from 'react';
import ReactModal from 'react-modal';

import Icon from '../Icon/Icon';
import styles from './Popup.module.css';

interface ILocalProps {
  title: React.ReactChild;
  isOpen: boolean;
  height?: number;
  contentLabel?: string;
  children: React.ReactChild | React.ReactChild[] | React.ReactChildren;
  onRequestClose(): void;
}

const appElement = document.getElementById('root')!;

class Popup extends React.Component<ILocalProps> {
  public render() {
    const {
      title,
      height,
      isOpen,
      contentLabel,
      children,
      onRequestClose,
    } = this.props;
    return (
      <ReactModal
        className={styles.popup}
        overlayClassName={styles.overlay}
        appElement={appElement}
        isOpen={isOpen}
        contentLabel={contentLabel}
        style={{ content: { height } }}
        onRequestClose={onRequestClose}
      >
        <div className={styles.header}>
          <div className={styles.title}>{title}</div>
          <Icon
            type="close"
            className={styles.close}
            onClick={onRequestClose}
          />
        </div>
        <div className={styles.content}>{children}</div>
      </ReactModal>
    );
  }
}

export default Popup;
