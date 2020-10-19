import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { RouteComponentProps, withRouter } from 'react-router';

import { ICommunication } from 'shared/utils/redux/communication';
import Confirm from 'shared/view/elements/Confirm/Confirm';
import { Icon, IconType } from 'shared/view/elements/Icon/Icon';

import styles from './BulkDeletionManager.module.css';

interface ILocalProps {
  entityName: string;
  entityIds: string[];
  deletingEntities: ICommunication;
  unselectEntityForDeleting(id: string): void;
  deleteEntities(entityIds: string[]): void;
  resetEntities(): void;
}

type AllProps = ILocalProps & RouteComponentProps<{}>;

const appRoot = document.querySelector('#root')!;

interface ILocalState {
  isShowConfirmation: boolean;
}

class BulkDeletionManager extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isShowConfirmation: false,
  };

  private sourceLocation = this.props.history.location;

  public componentDidMount() {
    window.addEventListener('keydown', this.resetEntitiesOnEsc);
  }

  public componentWillUnmount() {
    window.removeEventListener('keydown', this.resetEntitiesOnEsc);
    if (this.sourceLocation.pathname !== this.props.history.location.pathname) {
      this.props.resetEntities();
    }
  }

  public render() {
    const { entityName, entityIds, resetEntities } = this.props;
    const { isShowConfirmation } = this.state;

    return ReactDOM.createPortal(
      <div
        className={cn(styles.root, { [styles.visible]: entityIds.length > 0 })}
      >
        <this.Control
          theme="blue"
          icon="minus-solid"
          text="Deselect"
          onClick={resetEntities}
        />
        <this.Control
          theme="red"
          icon="close"
          text="Delete"
          dataTest="delete-entities-button"
          onClick={this.showConfirmation}
        />
        <div className={styles.controler_meta}>
          {entityIds.length > 1 ? (
            <span>
              {entityIds.length} {entityName.toLowerCase()}s selected
            </span>
          ) : (
            <span>
              {entityIds.length} {entityName.toLowerCase()} selected
            </span>
          )}
        </div>
        <Confirm
          title="Warning!"
          isOpen={isShowConfirmation}
          onCancel={this.closeConfirmation}
          onConfirm={this.deleteEntities}
        >
          {entityIds.length > 1 ? (
            <span>
              Are you sure? This will delete {entityIds.length}{' '}
              {entityName.toLowerCase()}s.
            </span>
          ) : (
            <span>
              Are you sure? This will delete {entityIds.length}{' '}
              {entityName.toLowerCase()}.
            </span>
          )}
        </Confirm>
        <div className={styles.deselectAllByKeyHint}>
          <div className={styles.deselectAllByKeyHint__key}>Esc</div>{' '}
          <div className={styles.deselectAllByKeyHint__text}>
            to deselect all
          </div>
        </div>
      </div>,
      appRoot
    );
  }

  @bind
  private Control({
    theme,
    icon,
    text,
    dataTest,
    onClick,
  }: {
    theme: 'blue' | 'red';
    icon: IconType;
    text: string;
    dataTest?: string;
    onClick(): void;
  }) {
    return (
      <div
        className={cn(styles.control, {
          [styles.control_blue]: theme === 'blue',
          [styles.control_red]: theme === 'red',
        })}
        data-test={dataTest}
        onClick={onClick}
      >
        <div className={styles.control__container}>
          <div className={styles.control__elem}>
            <Icon type={icon} className={styles.control__elemIcon} />
          </div>
          <div className={styles.control__text}>{text}</div>
        </div>
      </div>
    );
  }

  @bind
  private showConfirmation() {
    this.setState({
      isShowConfirmation: true,
    });
  }
  @bind
  private closeConfirmation() {
    this.setState({
      isShowConfirmation: false,
    });
  }

  @bind
  private deleteEntities() {
    this.props.deleteEntities(this.props.entityIds);
    this.setState({ isShowConfirmation: false });
  }

  @bind
  private resetEntitiesOnEsc(e: KeyboardEvent) {
    if (e.key === 'Escape') {
      this.props.resetEntities();
    }
  }
}

export default withRouter(BulkDeletionManager);
