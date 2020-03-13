import { bind } from 'decko';
import * as React from 'react';

import { EntityWithDescription } from 'core/shared/models/Description';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Tooltip from 'core/shared/view/elements/Tooltip/Tooltip';

import AddEditDescModal from './AddEditDescModal/AddEditDescModal';
import styles from './DescriptionManager.module.css';
import ActionIcon from '../../elements/ActionIcon/ActionIcon';

interface ILocalProps {
  entityId: string;
  entityType: EntityWithDescription;
  isReadOnly: boolean;
  isLoadingAccess: boolean;
  description: string;
  onAddOrEditDescription(
    id: string,
    description: string,
    entityType: EntityWithDescription
  ): void;
}

interface ILocalState {
  isShowModal: boolean;
  showTooltip: boolean;
}

type AllProps = ILocalProps;

class DescriptionManager extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isShowModal: false,
    showTooltip: false,
  };

  public render() {
    const { description, isLoadingAccess, isReadOnly } = this.props;
    return (
      <span className={styles.root} data-test="description">
        <span
          className={
            description ? styles.description : styles.description_empty
          }
          data-test="description-text"
        >
          {(() => {
            if (description) {
              return description;
            }
            if (!isLoadingAccess && !isReadOnly) {
              return 'Description';
            }
            return '';
          })()}
          {!isLoadingAccess && !isReadOnly && (
            <span>
              <div
                className={styles.desc_action_block}
                onClick={this.showModal}
                data-test="description-open-editor-button"
              >
                <ActionIcon iconType="pencil" />
              </div>
              <AddEditDescModal
                description={description}
                isOpen={this.state.isShowModal}
                onAddEdit={this.addEditDesc}
                onClose={this.closeModal}
              />
            </span>
          )}
          {!isLoadingAccess && isReadOnly && (
            <span>
              <Tooltip
                content={'Read Only Project'}
                visible={this.state.showTooltip}
              >
                <div
                  className={styles.desc_readonly}
                  onMouseOver={this.showTooltip}
                  onMouseOut={this.hideTooltip}
                >
                  <ActionIcon iconType="eye" />
                </div>
              </Tooltip>
            </span>
          )}
        </span>
      </span>
    );
  }

  @bind
  private showTooltip(event: React.MouseEvent) {
    event.preventDefault();
    this.setState({ showTooltip: true });
  }

  @bind
  private hideTooltip(event: React.MouseEvent) {
    event.preventDefault();
    this.setState({ showTooltip: false });
  }

  @bind
  private showModal(event: React.MouseEvent<HTMLDivElement, MouseEvent>) {
    event.preventDefault();
    this.setState({ isShowModal: true });
  }

  @bind
  private closeModal() {
    this.setState({ isShowModal: false });
  }

  @bind
  private addEditDesc(descCreated: string) {
    const { entityId: id, entityType } = this.props;
    this.props.onAddOrEditDescription(id, descCreated, entityType);
    this.setState({ isShowModal: false });
  }
}

export type IDescriptionManagerLocalProps = ILocalProps;
export default DescriptionManager;
