import cn from 'classnames';
import * as React from 'react';

import { ReactComponent as ArrowDownSolid } from './imgs/arrow-down-solid.svg';
import { ReactComponent as ArrowLeft } from './imgs/arrow-left.svg';
import { ReactComponent as ArrowRightCircled } from './imgs/arrow-right-circled.svg';
import { ReactComponent as ArrowRight } from './imgs/arrow-right.svg';
import { ReactComponent as ArrowUpSolid } from './imgs/arrow-up-solid.svg';
import { ReactComponent as Attribute } from './imgs/attribute.svg';
import { ReactComponent as BellSolid } from './imgs/bell-solid.svg';
import { ReactComponent as BinocularsTilted } from './imgs/binoculars-tilted.svg';
import { ReactComponent as Binoculars } from './imgs/binoculars.svg';
import { ReactComponent as Bitbucket } from './imgs/bitbucket.svg';
import { ReactComponent as Book } from './imgs/book.svg';
import { ReactComponent as Bookmarks } from './imgs/bookmark.svg';
import { ReactComponent as Cancel } from './imgs/cancel.svg';
import { ReactComponent as CaretDown } from './imgs/caret-down.svg';
import { ReactComponent as CaretRight } from './imgs/caret-right.svg';
import { ReactComponent as CaretUp } from './imgs/caret-up.svg';
import { ReactComponent as CheckCircle } from './imgs/check-circle-solid.svg';
import { ReactComponent as CheckSolid } from './imgs/check-solid.svg';
import { ReactComponent as Check } from './imgs/check.svg';
import { ReactComponent as Checkmark } from './imgs/checkmark.svg';
import { ReactComponent as Close } from './imgs/close.svg';
import { ReactComponent as Code } from './imgs/code.svg';
import { ReactComponent as Codepen } from './imgs/codepen.svg';
import { ReactComponent as Cog } from './imgs/cog.svg';
import { ReactComponent as Comment } from './imgs/comment.svg';
import { ReactComponent as CompareArrows } from './imgs/compare-arrows.svg';
import { ReactComponent as CopyToClipboard } from './imgs/copy-to-clipboard.svg';
import { ReactComponent as Cube } from './imgs/cube.svg';
import { ReactComponent as Database } from './imgs/database.svg';
import { ReactComponent as Datasets } from './imgs/datasets.svg';
import { ReactComponent as DeviceHub } from './imgs/device-hub.svg';
import { ReactComponent as DoubleDownLite } from './imgs/double-down-lite.svg';
import { ReactComponent as ArrowDownLite } from './imgs/down-arrow-lite.svg';
import { ReactComponent as ArrowDown } from './imgs/down-arrow.svg';
import { ReactComponent as DragAndDrop } from './imgs/drag-and-drop.svg';
import { ReactComponent as Email } from './imgs/email.svg';
import { ReactComponent as EndpointActive } from './imgs/endpoint-active.svg';
import { ReactComponent as EndpointError } from './imgs/endpoint-error.svg';
import { ReactComponent as EndpointInactive } from './imgs/endpoint-inactive.svg';
import { ReactComponent as Error } from './imgs/error.svg';
import { ReactComponent as ExclamationTriangleLite } from './imgs/exclamation-triangle-lite.svg';
import { ReactComponent as ExclamationTriangle } from './imgs/exclamation-triangle.svg';
import { ReactComponent as ExperimentRun } from './imgs/experiment-run.svg';
import { ReactComponent as Experiment } from './imgs/experiment.svg';
import { ReactComponent as ExternalLink } from './imgs/external-link.svg';
import { ReactComponent as Eye } from './imgs/eye.svg';
import { ReactComponent as Favorite } from './imgs/favorite.svg';
import { ReactComponent as File } from './imgs/file.svg';
import { ReactComponent as FilterLight } from './imgs/filter-light.svg';
import { ReactComponent as Filter } from './imgs/filter.svg';
import { ReactComponent as Folder } from './imgs/folder.svg';
import { ReactComponent as Github } from './imgs/github-logo.svg';
import { ReactComponent as GlobeSolid } from './imgs/globe-solid.svg';
import { ReactComponent as Google } from './imgs/google.svg';
import { ReactComponent as Heart } from './imgs/heart.svg';
import { ReactComponent as HelpOutline } from './imgs/help-outline.svg';
import { ReactComponent as Home } from './imgs/home.svg';
import { ReactComponent as Image } from './imgs/image.svg';
import { ReactComponent as Key } from './imgs/key.svg';
import { ReactComponent as Leave } from './imgs/leave.svg';
import { ReactComponent as Link } from './imgs/link.svg';
import { ReactComponent as LinkedIn } from './imgs/linked-in-logo.svg';
import { ReactComponent as List } from './imgs/list.svg';
import { ReactComponent as LockSolid } from './imgs/lock-solid.svg';
import { ReactComponent as SAML } from './imgs/lock-solid.svg';
import { ReactComponent as Login } from './imgs/login.svg';
import { ReactComponent as Medium } from './imgs/medium-logo.svg';
import { ReactComponent as MinusSolid } from './imgs/minus-solid.svg';
import { ReactComponent as Monitoring } from './imgs/monitoringg.svg';
import { ReactComponent as OrganizationWorkspace } from './imgs/organization-workspace.svg';
import { ReactComponent as Pencil } from './imgs/pencil.svg';
import { ReactComponent as PersonalWorkspace } from './imgs/personal-workspace.svg';
import { ReactComponent as RoundedPlusFilled } from './imgs/plus-rounded-filled.svg';
import { ReactComponent as RoundedPlus } from './imgs/plus-rounded.svg';
import { ReactComponent as Plus } from './imgs/plus.svg';
import { ReactComponent as Query } from './imgs/query.svg';
import { ReactComponent as ReadOnly } from './imgs/read-only.svg';
import { ReactComponent as Releases } from './imgs/releases.svg';
import { ReactComponent as Repository } from './imgs/repository.svg';
import { ReactComponent as Reset } from './imgs/reset.svg';
import { ReactComponent as RoundedPlusLight } from './imgs/rounded-plus-light.svg';
import { ReactComponent as Seach } from './imgs/search.svg';
import { ReactComponent as ShareArrow } from './imgs/share-arrow.svg';
import { ReactComponent as ShareChange } from './imgs/share-change.svg';
import { ReactComponent as ShareDelete } from './imgs/share-delete.svg';
import { ReactComponent as ShareRead } from './imgs/share-read.svg';
import { ReactComponent as ShareWrite } from './imgs/share-write.svg';
import { ReactComponent as Share } from './imgs/share.svg';
import { ReactComponent as SortAscending } from './imgs/sort-ascending.svg';
import { ReactComponent as SortDescending } from './imgs/sort-descending.svg';
import { ReactComponent as SortSolid } from './imgs/sort-solid.svg';
import { ReactComponent as Trash } from './imgs/trash.svg';
import { ReactComponent as Twitter } from './imgs/twitter-logo.svg';
import { ReactComponent as ArrowUpLite } from './imgs/up-arrow-lite.svg';
import { ReactComponent as ArrowUp } from './imgs/up-arrow.svg';
import { ReactComponent as UpDownArrow } from './imgs/up-down-arrow.svg';
import { ReactComponent as Upload } from './imgs/upload.svg';
import { ReactComponent as User } from './imgs/user.svg';
import { ReactComponent as Users } from './imgs/users.svg';
import { ReactComponent as Hyperparameters } from './imgs/hyperparameters.svg';
import { ReactComponent as Metrics } from './imgs/metrics.svg';
import { ReactComponent as Attributes } from './imgs/attributes.svg';
import { ReactComponent as Artifacts } from './imgs/artifacts.svg';
import { ReactComponent as Observations } from './imgs/observations.svg';
import { ReactComponent as Download } from './imgs/download.svg';
import { ReactComponent as Delete } from './imgs/delete.svg';
import { ReactComponent as Preview } from './imgs/preview.svg';

import styles from './Icon.module.css';

interface ILocalProps {
  type: IconType;
  className?: string;
  dataTest?: string;
  dataId?: string;
  onClick?(e: React.MouseEvent): void;
}

export type IconType = keyof typeof icons;
const icons = {
  home: Home,
  'drag-and-drop': DragAndDrop,
  'filter-light': FilterLight,
  checkmark: Checkmark,
  search: Seach,
  'caret-down': CaretDown,
  'caret-up': CaretUp,
  'caret-right': CaretRight,
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
  folder: Folder,
  bookmarks: Bookmarks,
  'device-hub': DeviceHub,
  link: Link,
  'help-outline': HelpOutline,
  'organization-workspace': OrganizationWorkspace,
  user: User,
  users: Users,
  leave: Leave,
  'personal-workspace': PersonalWorkspace,
  repository: Repository,
  file: File,
  'bell-solid': BellSolid,
  datasets: Datasets,
  monitoring: Monitoring,
  experiment: Experiment,
  'experiment-run': ExperimentRun,
  SAML: SAML,
  favorite: Favorite,
  'sort-ascending': SortAscending,
  'sort-descending': SortDescending,
  releases: Releases,
  hyperpameters: Hyperparameters,
  metrics: Metrics,
  attributes: Attributes,
  artifacts: Artifacts,
  observations: Observations,
  download: Download,
  'endpoint-active': EndpointActive,
  'endpoint-error': EndpointError,
  'endpoint-inactive': EndpointInactive,
  preview: Preview,
  delete: Delete,
};

export class Icon extends React.PureComponent<ILocalProps> {
  public render() {
    const { className, type, dataTest, onClick, dataId } = this.props;
    const IconComponent = icons[type];
    return (
      <i
        className={cn(styles.icon, className)}
        data-test={dataTest}
        onClick={onClick}
        data-id={dataId}
      >
        <IconComponent />
      </i>
    );
  }
}
