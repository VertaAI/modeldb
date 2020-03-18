import cn from 'classnames';
import * as React from 'react';

import { ReactComponent as ArrowDownSolid } from './imgs/arrow-down-solid.svg';
import { ReactComponent as ArrowLeft } from './imgs/arrow-left.svg';
import { ReactComponent as ArrowRightCircled } from './imgs/arrow-right-circled.svg';
import { ReactComponent as ArrowRight } from './imgs/arrow-right.svg';
import { ReactComponent as ArrowUpSolid } from './imgs/arrow-up-solid.svg';
import { ReactComponent as Attribute } from './imgs/attribute.svg';
import { ReactComponent as Ban } from './imgs/ban.svg';
import { ReactComponent as BinocularsTilted } from './imgs/binoculars-tilted.svg';
import { ReactComponent as Binoculars } from './imgs/binoculars.svg';
import { ReactComponent as Bitbucket } from './imgs/bitbucket.svg';
import { ReactComponent as Book } from './imgs/book.svg';
import { ReactComponent as Bookmarks } from './imgs/bookmark.svg';
import { ReactComponent as Cancel } from './imgs/cancel.svg';
import { ReactComponent as CaretDown } from './imgs/caret-down.svg';
import { ReactComponent as CaretUp } from './imgs/caret-up.svg';
import { ReactComponent as CheckCircle } from './imgs/check-circle-solid.svg';
import { ReactComponent as CheckSolid } from './imgs/check-solid.svg';
import { ReactComponent as Check } from './imgs/check.svg';
import { ReactComponent as Close } from './imgs/close.svg';
import { ReactComponent as Code } from './imgs/code.svg';
import { ReactComponent as Codepen } from './imgs/codepen.svg';
import { ReactComponent as Cog } from './imgs/cog.svg';
import { ReactComponent as Comment } from './imgs/comment.svg';
import { ReactComponent as CompareArrows } from './imgs/compare-arrows.svg';
import { ReactComponent as CopyToClipboard } from './imgs/copy-to-clipboard.svg';
import { ReactComponent as Cube } from './imgs/cube.svg';
import { ReactComponent as Database } from './imgs/database.svg';
import { ReactComponent as DeviceHub } from './imgs/device-hub.svg';
import { ReactComponent as DoubleDownLite } from './imgs/double-down-lite.svg';
import { ReactComponent as ArrowDownLite } from './imgs/down-arrow-lite.svg';
import { ReactComponent as ArrowDown } from './imgs/down-arrow.svg';
import { ReactComponent as Email } from './imgs/email.svg';
import { ReactComponent as Error } from './imgs/error.svg';
import { ReactComponent as ExclamationTriangleLite } from './imgs/exclamation-triangle-lite.svg';
import { ReactComponent as ExclamationTriangle } from './imgs/exclamation-triangle.svg';
import { ReactComponent as ExternalLink } from './imgs/external-link.svg';
import { ReactComponent as Eye } from './imgs/eye.svg';
import { ReactComponent as Filter } from './imgs/filter.svg';
import { ReactComponent as Folder } from './imgs/folder.svg';
import { ReactComponent as Github } from './imgs/github-logo.svg';
import { ReactComponent as GlobeSolid } from './imgs/globe-solid.svg';
import { ReactComponent as Google } from './imgs/google.svg';
import { ReactComponent as Heart } from './imgs/heart.svg';
import { ReactComponent as HelpOutline } from './imgs/help-outline.svg';
import { ReactComponent as Image } from './imgs/image.svg';
import { ReactComponent as Key } from './imgs/key.svg';
import { ReactComponent as Link } from './imgs/link.svg';
import { ReactComponent as LinkedIn } from './imgs/linked-in-logo.svg';
import { ReactComponent as List } from './imgs/list.svg';
import { ReactComponent as LockSolid } from './imgs/lock-solid.svg';
import { ReactComponent as Login } from './imgs/login.svg';
import { ReactComponent as Medium } from './imgs/medium-logo.svg';
import { ReactComponent as MinusSolid } from './imgs/minus-solid.svg';
import { ReactComponent as Pencil } from './imgs/pencil.svg';
import { ReactComponent as RoundedPlusFilled } from './imgs/plus-rounded-filled.svg';
import { ReactComponent as RoundedPlus } from './imgs/plus-rounded.svg';
import { ReactComponent as Plus } from './imgs/plus.svg';
import { ReactComponent as Query } from './imgs/query.svg';
import { ReactComponent as ReadOnly } from './imgs/read-only.svg';
import { ReactComponent as Reset } from './imgs/reset.svg';
import { ReactComponent as RoundedPlusLight } from './imgs/rounded-plus-light.svg';
import { ReactComponent as Seach } from './imgs/search.svg';
import { ReactComponent as ShareArrow } from './imgs/share-arrow.svg';
import { ReactComponent as ShareChange } from './imgs/share-change.svg';
import { ReactComponent as ShareDelete } from './imgs/share-delete.svg';
// todo rename
import { ReactComponent as ShareRead } from './imgs/share-read.svg';
import { ReactComponent as ShareWrite } from './imgs/share-write.svg';
import { ReactComponent as Share } from './imgs/share.svg';
import { ReactComponent as SortSolid } from './imgs/sort-solid.svg';
import { ReactComponent as Trash } from './imgs/trash.svg';
import { ReactComponent as Twitter } from './imgs/twitter-logo.svg';
import { ReactComponent as ArrowUpLite } from './imgs/up-arrow-lite.svg';
import { ReactComponent as ArrowUp } from './imgs/up-arrow.svg';
import { ReactComponent as UpDownArrow } from './imgs/up-down-arrow.svg';
import { ReactComponent as Upload } from './imgs/upload.svg';
import { ReactComponent as User } from './imgs/user.svg';
import { ReactComponent as Users } from './imgs/users.svg';
import { ReactComponent as Workspaces } from './imgs/workspaces.svg';
import { ReactComponent as Leave } from './imgs/leave.svg';

import styles from './Icon.module.css';

interface ILocalProps {
  type: IconType;
  className?: string;
  dataTest?: string;
  dataId?: string;
  onClick?(e: React.MouseEvent): void;
}

export type IconType =
  | 'search'
  | 'cannot-deploy'
  | 'caret-down'
  | 'caret-up'
  | 'medium'
  | 'github'
  | 'linkedIn'
  | 'twitter'
  | 'filter'
  | 'heart'
  | 'image'
  | 'codepen'
  | 'cog'
  | 'arrow-right'
  | 'arrow-left'
  | 'arrow-up'
  | 'arrow-down'
  | 'error'
  | 'check'
  | 'check-solid'
  | 'check-circle'
  | 'read-only'
  | 'share-read'
  | 'share-write'
  | 'share-change'
  | 'share-delete'
  | 'share-arrow'
  | 'share'
  | 'login'
  | 'cancel'
  | 'upload'
  | 'close'
  | 'reset'
  | 'key'
  | 'cube'
  | 'database'
  | 'external-link'
  | 'exclamation-triangle'
  | 'exclamation-triangle-lite'
  | 'compare-arrows'
  | 'copy-to-clipboard'
  | 'comment'
  | 'trash'
  | 'pencil'
  | 'rounded-plus'
  | 'rounded-plus-light'
  | 'rounded-plus-filled'
  | 'query'
  | 'code'
  | 'eye'
  | 'book'
  | 'attribute'
  | 'binoculars'
  | 'binoculars-tilted'
  | 'up-down-arrow'
  | 'double-down-lite'
  | 'sort-solid'
  | 'plus'
  | 'arrow-up-lite'
  | 'arrow-down-lite'
  | 'arrow-up-solid'
  | 'arrow-down-solid'
  | 'arrow-right-circle'
  | 'minus-solid'
  | 'list'
  | 'globe-solid'
  | 'lock-solid'
  | 'bitbucket'
  | 'google'
  | 'email'
  | 'folder'
  | 'bookmarks'
  | 'device-hub'
  | 'link'
  | 'help-outline'
  | 'workspaces'
  | 'user'
  | 'users'
  | 'leave';

export class Icon extends React.PureComponent<ILocalProps> {
  public render() {
    const { className, type, dataTest, onClick, dataId } = this.props;
    const IconComponent = this.getIconComponent();
    return (
      <i
        className={cn(styles.icon, className)}
        data-test={dataTest}
        onClick={onClick}
        data-id={dataId}
      >
        {type === 'cannot-deploy' ? <CannotDeployIcon /> : <IconComponent />}
      </i>
    );
  }

  private getIconComponent() {
    const icons: Record<IconType, any> = {
      search: Seach,
      'caret-down': CaretDown,
      'caret-up': CaretUp,
      medium: Medium,
      github: Github,
      linkedIn: LinkedIn,
      twitter: Twitter,
      filter: Filter,
      heart: Heart,
      image: Image,
      codepen: Codepen,
      cog: Cog,
      'arrow-right': ArrowRight,
      'arrow-left': ArrowLeft,
      'arrow-up': ArrowUp,
      'arrow-down': ArrowDown,
      error: Error,
      check: Check,
      'check-solid': CheckSolid,
      'check-circle': CheckCircle,
      'read-only': ReadOnly,
      'share-read': ShareRead,
      'share-write': ShareWrite,
      'share-change': ShareChange,
      'share-delete': ShareDelete,
      'share-arrow': ShareArrow,
      share: Share,
      login: Login,
      cancel: Cancel,
      upload: Upload,
      close: Close,
      reset: Reset,
      key: Key,
      cube: Cube,
      database: Database,
      'external-link': ExternalLink,
      'exclamation-triangle': ExclamationTriangle,
      'exclamation-triangle-lite': ExclamationTriangleLite,
      'compare-arrows': CompareArrows,
      'copy-to-clipboard': CopyToClipboard,
      comment: Comment,
      trash: Trash,
      pencil: Pencil,
      'rounded-plus': RoundedPlus,
      'rounded-plus-light': RoundedPlusLight,
      'rounded-plus-filled': RoundedPlusFilled,
      query: Query,
      code: Code,
      eye: Eye,
      book: Book,
      attribute: Attribute,
      binoculars: Binoculars,
      'binoculars-tilted': BinocularsTilted,
      'up-down-arrow': UpDownArrow,
      'arrow-up-lite': ArrowUpLite,
      'arrow-down-lite': ArrowDownLite,
      'double-down-lite': DoubleDownLite,
      'sort-solid': SortSolid,
      plus: Plus,
      'arrow-down-solid': ArrowDownSolid,
      'arrow-up-solid': ArrowUpSolid,
      'arrow-right-circle': ArrowRightCircled,
      'minus-solid': MinusSolid,
      list: List,
      'globe-solid': GlobeSolid,
      'lock-solid': LockSolid,
      bitbucket: Bitbucket,
      email: Email,
      google: Google,
      'cannot-deploy': CannotDeployIcon,
      folder: Folder,
      bookmarks: Bookmarks,
      'device-hub': DeviceHub,
      link: Link,
      'help-outline': HelpOutline,
      workspaces: Workspaces,
      user: User,
      users: Users,
      leave: Leave,
    };
    return icons[this.props.type];
  }
}

const CannotDeployIcon = () => {
  return (
    <i
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
      }}
    >
      <Icon type="upload" />
      <div style={{ position: 'absolute' }}>
        <Ban />
      </div>
    </i>
  );
};
