import cn from 'classnames';
import * as React from 'react';

import { ReactComponent as CirclePreloader } from './imgs/circle-preloader.svg';
import dotsPreloaderSrc from './imgs/dots-preloader.gif';
import styles from './Preloader.module.css';

type ILocalProps =
  | { variant: 'dots' }
  | { variant: 'circle'; dynamicSize: boolean; theme: 'light' | 'blue' };

class Preloader extends React.PureComponent<ILocalProps> {
  public render() {
    const className = cn(styles.preloader, {
      [styles.dynamic_size]:
        this.props.variant === 'circle' && this.props.dynamicSize,
      [styles.theme_blue]:
        this.props.variant === 'circle' && this.props.theme === 'blue',
      [styles.theme_light]:
        this.props.variant === 'circle' && this.props.theme === 'light',
    });

    if (this.props.variant === 'dots') {
      return (
        <img
          className={className}
          src={dotsPreloaderSrc}
          data-test="preloader"
          alt="preloader"
        />
      );
    }
    return <CirclePreloader className={className} />;
  }
}

export default Preloader;
