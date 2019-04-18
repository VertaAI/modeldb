import * as React from 'react';
import ReactModal from 'react-modal';

import closeIcon from './images/close.svg';
import styles from './ModalCard.module.css';

interface ILocalProps {
  isOpen: boolean;
  height?: number;
  contentLabel?: string;
  children?: React.ReactChild | React.ReactChildren;
  onRequestClose(): void;
}

const appElement = document.getElementById('root')!;

class ModalCard extends React.Component<ILocalProps> {
  public render() {
    const {
      height,
      isOpen,
      contentLabel,
      children,
      onRequestClose,
    } = this.props;
    return (
      <ReactModal
        className={styles.modalCard}
        overlayClassName={styles.overlay}
        appElement={appElement}
        isOpen={isOpen}
        contentLabel={contentLabel}
        style={{ content: { height } }}
        onRequestClose={onRequestClose}
      >
        <div className={styles.header}>
          <img
            className={styles.close}
            src={closeIcon}
            onClick={onRequestClose}
          />
        </div>
        <div className={styles.content}>{children}</div>
      </ReactModal>
    );
  }
}

export default ModalCard;
