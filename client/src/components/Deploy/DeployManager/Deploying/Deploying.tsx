import * as React from 'react';

import Popup from 'components/shared/Popup/Popup';

import styles from './Deploying.module.css';
import preloaderSrc from './imgs/preloader.svg';

interface ILocalProps {
  onClose(): void;
}

class Deploying extends React.PureComponent<ILocalProps> {
  public render() {
    const { onClose } = this.props;
    return (
      <Popup isOpen={true} title="" onRequestClose={onClose}>
        <div className={styles.deploying}>
          <img className={styles.preloader} src={preloaderSrc} />
          <span className={styles.description}>
            We are deploying the model right now...
          </span>
        </div>
      </Popup>
    );
  }
}

export default Deploying;
