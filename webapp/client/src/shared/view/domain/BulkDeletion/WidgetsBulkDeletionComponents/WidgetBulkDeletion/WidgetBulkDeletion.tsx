import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { ICommunication } from 'shared/utils/redux/communication';
import Fai from 'shared/view/elements/Fai/Fai';
import { Icon } from 'shared/view/elements/Icon/Icon';

import styles from './WidgetBulkDeletion.module.css';

interface ILocalProps {
  id: string;
  isEnabled: boolean;
  isSelected: boolean;
  deleting: ICommunication;
  children: (togglerElement?: React.ReactElement) => React.ReactNode;
  selectEntity(id: string): void;
  unselectEntity(id: string): void;
}

type AllProps = ILocalProps;

class WidgetBulkDeletion extends React.PureComponent<AllProps> {
  public render() {
    const { children, isEnabled, isSelected, deleting } = this.props;

    return !isEnabled ? (
      children(undefined)
    ) : (
      <div
        className={cn(styles.root, { [styles.muted_widget]: isSelected })}
        data-test="widget-bulk-deletion"
      >
        <div
          className={cn(styles.entity, {
            [styles.entity_selected]: isSelected,
            [styles.entity_deleting]: deleting.isRequesting,
          })}
        >
          {children(
            <Fai
              theme={isSelected ? 'red' : 'tertiary'}
              variant="outlined"
              icon={<Icon type={isSelected ? 'minus-solid' : 'trash'} />}
              dataTest={'toggler-entity-for-deleting'}
              onClick={this.toggleEntityForDeleting}
              unmutedIcon={isSelected}
            />
          )}
        </div>
        <div
          className={isSelected ? styles.togglerContainer : styles.hideToggler}
        >
          <div className={styles.toggler}>
            <Icon type={'trash'} className={styles.trash_icon} />
          </div>
        </div>
      </div>
    );
  }

  @bind
  private toggleEntityForDeleting() {
    const { id, isSelected, selectEntity, unselectEntity } = this.props;
    if (isSelected) {
      unselectEntity(id);
    } else {
      selectEntity(id);
    }
  }
}

export default WidgetBulkDeletion;
