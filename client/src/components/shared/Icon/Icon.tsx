import cn from 'classnames';
import * as React from 'react';

import { ReactComponent as CaretDown } from './imgs/caret-down.svg';
import { ReactComponent as CaretUp } from './imgs/caret-up.svg';
import { ReactComponent as Codepen } from './imgs/codepen.svg';
import { ReactComponent as Cog } from './imgs/cog.svg';
import { ReactComponent as Facebook } from './imgs/facebook-logo.svg';
import { ReactComponent as Filter } from './imgs/filter.svg';
import { ReactComponent as Github } from './imgs/github-logo.svg';
import { ReactComponent as Heart } from './imgs/heart.svg';
import { ReactComponent as Image } from './imgs/image.svg';
import { ReactComponent as LinkedIn } from './imgs/linkedIN-logo.svg';
import { ReactComponent as Seach } from './imgs/search.svg';
import { ReactComponent as Twitter } from './imgs/twitter-logo.svg';

import styles from './Icon.module.css';

interface ILocalProps {
  type: IconType;
  className?: string;
}

type IconType =
  | 'search'
  | 'caret-down'
  | 'caret-up'
  | 'facebook'
  | 'github'
  | 'linkedIn'
  | 'twitter'
  | 'filter'
  | 'heart'
  | 'image'
  | 'codepen'
  | 'cog';

class Icon extends React.PureComponent<ILocalProps> {
  public render() {
    const { className } = this.props;
    const IconComponent = this.getIconComponent();
    return (
      <i className={cn(styles.icon, className)}>
        <IconComponent />
      </i>
    );
  }

  private getIconComponent() {
    return ({
      search: Seach,
      'caret-down': CaretDown,
      'caret-up': CaretUp,
      facebook: Facebook,
      github: Github,
      linkedIn: LinkedIn,
      twitter: Twitter,
      filter: Filter,
      heart: Heart,
      image: Image,
      codepen: Codepen,
      cog: Cog,
    } as Record<IconType, any>)[this.props.type];
  }
}

export default Icon;
