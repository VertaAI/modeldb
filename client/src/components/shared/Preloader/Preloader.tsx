import cn from 'classnames';
import * as React from 'react';

import circlePreloaderSrc from './imgs/circle-preloader.svg';
import dotsPreloaderSrc from './imgs/dots-preloader.gif';
import styles from './Preloader.module.css';

interface ILocalProps {
  variant: 'dots' | 'circle';
  dynamicSize?: boolean;
}

class Preloader extends React.PureComponent<ILocalProps> {
  public render() {
    const { variant, dynamicSize = false } = this.props;
    const preloaderSrc =
      variant === 'dots' ? dotsPreloaderSrc : circlePreloaderSrc;
    return (
      <img
        className={cn(styles.preloader, {
          [styles.dynamic_size]: dynamicSize,
        })}
        src={preloaderSrc}
      />
    );
  }
}

export default Preloader;
