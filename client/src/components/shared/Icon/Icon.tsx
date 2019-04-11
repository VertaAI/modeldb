import cn from 'classnames';
import * as React from 'react';

import { ReactComponent as ArrowRight } from './imgs/arrow-right.svg';
import { ReactComponent as CaretDown } from './imgs/caret-down.svg';
import { ReactComponent as CaretUp } from './imgs/caret-up.svg';
import { ReactComponent as Check } from './imgs/check.svg';
import { ReactComponent as Close } from './imgs/close.svg';
import { ReactComponent as Codepen } from './imgs/codepen.svg';
import { ReactComponent as Cog } from './imgs/cog.svg';
import { ReactComponent as Error } from './imgs/error.svg';
import { ReactComponent as Facebook } from './imgs/facebook-logo.svg';
import { ReactComponent as Filter } from './imgs/filter.svg';
import { ReactComponent as Github } from './imgs/github-logo.svg';
import { ReactComponent as Heart } from './imgs/heart.svg';
import { ReactComponent as Image } from './imgs/image.svg';
import { ReactComponent as Key } from './imgs/key.svg';
import { ReactComponent as LinkedIn } from './imgs/linkedIN-logo.svg';
import { ReactComponent as ReadOnly } from './imgs/read-only.svg';
import { ReactComponent as Seach } from './imgs/search.svg';
import { ReactComponent as ShareChange } from './imgs/share-change.svg';
import { ReactComponent as ShareDelete } from './imgs/share-delete.svg';
import { ReactComponent as ShareRead } from './imgs/share-read.svg';
import { ReactComponent as ShareWrite } from './imgs/share-write.svg';
import { ReactComponent as Twitter } from './imgs/twitter-logo.svg';
import { ReactComponent as Upload } from './imgs/upload.svg';

import styles from './Icon.module.css';

interface ILocalProps {
  type: IconType;
  className?: string;
  onClick?(): void;
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
  | 'cog'
  | 'arrow-right'
  | 'error'
  | 'check'
  | 'read-only'
  | 'share-read'
  | 'share-write'
  | 'share-change'
  | 'share-delete'
  | 'upload'
  | 'close'
  | 'key';

class Icon extends React.PureComponent<ILocalProps> {
  public render() {
    const { className, onClick } = this.props;
    const IconComponent = this.getIconComponent();
    return (
      <i className={cn(styles.icon, className)} onClick={onClick}>
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
      'arrow-right': ArrowRight,
      error: Error,
      check: Check,
      'read-only': ReadOnly,
      'share-read': ShareRead,
      'share-write': ShareWrite,
      'share-change': ShareChange,
      'share-delete': ShareDelete,
      upload: Upload,
      close: Close,
      key: Key,
    } as Record<
      IconType,
      React.FunctionComponent<React.SVGProps<SVGSVGElement>>
    >)[this.props.type];
  }
}

export default Icon;
