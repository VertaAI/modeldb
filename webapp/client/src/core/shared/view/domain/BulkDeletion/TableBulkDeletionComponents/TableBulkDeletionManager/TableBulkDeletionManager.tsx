import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { withRouter, RouteComponentProps } from 'react-router';

import { ICommunication } from 'core/shared/utils/redux/communication';
import Confirm from 'core/shared/view/elements/Confirm/Confirm';
import { Icon, IconType } from 'core/shared/view/elements/Icon/Icon';

import styles from './TableBulkDeletionManager.module.css';

interface ILocalProps {
  entityName: string;
  entityIds: string[];
  deletingEntities: ICommunication;
  unselectEntityForDeleting(id: string): void;
  deleteEntities(entityIds: string[]): void;
  resetEntities(): void;
}

type AllProps = ILocalProps & RouteComponentProps<{}>;

interface ILocalState {
  isShowConfirmation: boolean;
}

class TableBulkDeletionManager extends React.PureComponent<
  AllProps,
  ILocalState
> {
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

    return (
      <div className={cn(styles.root)}>
        <this.Control
          theme="blue"
          icon="minus-solid"
          text="Deselect"
          onClick={resetEntities}
        />
        {entityIds.length > 0 && (
          <>
            <this.Control
              theme="red"
              icon="close"
              text="Delete"
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
              confirmButtonTheme="red"
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
          </>
        )}
      </div>
    );
  }

  @bind
  private Control({
    theme,
    icon,
    text,
    onClick,
  }: {
    theme: 'blue' | 'red';
    icon: IconType;
    text: string;
    onClick(): void;
  }) {
    return (
      <div
        className={cn(styles.control, {
          [styles.control_blue]: theme === 'blue',
          [styles.control_red]: theme === 'red',
        })}
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

export default withRouter(TableBulkDeletionManager);
