import * as React from 'react';
import ReactModal from 'react-modal';

import closeIcon from './images/close.svg';
import styles from './Popup.module.css';

interface IProps {
  title: React.ReactChild;
  isOpen: boolean;
  contentLabel?: string;
  children: React.ReactChild | React.ReactChildren;
  onRequestClose(): void;
}

const appElement = document.getElementById('root')!;

class Popup extends React.Component<IProps> {
  public render() {
    const { title, isOpen, contentLabel, children, onRequestClose } = this.props;
    return (
      <ReactModal
        className={styles.popup}
        overlayClassName={styles.overlay}
        appElement={appElement}
        isOpen={isOpen}
        contentLabel={contentLabel}
        onRequestClose={onRequestClose}
      >
        <div className={styles.header}>
          <div className={styles.title}>{title}</div>
          <img className={styles.close} src={closeIcon} onClick={onRequestClose} />
        </div>
        <div className={styles.content}>{children}</div>
      </ReactModal>
    );
  }
}

export default Popup;
