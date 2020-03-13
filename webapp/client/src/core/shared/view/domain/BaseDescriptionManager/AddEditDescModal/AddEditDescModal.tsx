import { bind } from 'decko';
import * as React from 'react';

import Button from 'core/shared/view/elements/Button/Button';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';

import styles from './AddEditDescModal.module.css';

interface ILocalProps {
  description: string;
  isOpen: boolean;
  onAddEdit(description: string): void;
  onClose(): void;
}

interface ILocalState {
  descCreated: string;
  descLimitReached: boolean;
}

class AddEditDescModal extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    descCreated: this.props.description || '',
    descLimitReached: false,
  };

  public render() {
    const { isOpen, onClose, description } = this.props;
    return (
      <Popup
        title={
          description && description.length > 0
            ? 'Edit Description'
            : 'Add Description'
        }
        titleIcon={
          description && description.length > 0
            ? 'pencil'
            : 'rounded-plus-filled'
        }
        contentLabel="description-action"
        isOpen={isOpen}
        onRequestClose={onClose}
      >
        <div className={styles.root} data-test="description-editor">
          <div className={styles.message}>
            <textarea
              rows={5}
              cols={50}
              name="desc-submitted"
              value={this.state.descCreated}
              id="descInput"
              className={styles.input}
              onChange={this.updateInput}
              autoFocus={true}
              maxLength={256}
              data-test="description-input"
            />
            {this.state.descLimitReached && (
              <div className={styles.prompt_desc_limit}>
                <Icon type="exclamation-triangle" /> maximum character limit
                reached
              </div>
            )}
          </div>
          <div className={styles.actions}>
            <div className={styles.action}>
              <Button theme="secondary" onClick={onClose}>
                Cancel
              </Button>
            </div>
            <div className={styles.action}>
              <Button
                onClick={this.onAddEditDesc}
                dataTest="description-update-button"
              >
                Save
              </Button>
            </div>
          </div>
        </div>
      </Popup>
    );
  }

  @bind
  private updateInput(event: React.ChangeEvent<HTMLTextAreaElement>) {
    if (event.target.value.length === 256) {
      this.setState({ descLimitReached: true });
    } else {
      this.setState({ descLimitReached: false });
    }
    this.setState({ descCreated: event.target.value });
  }

  @bind
  private onAddEditDesc() {
    this.props.onAddEdit(this.state.descCreated);
    this.props.onClose();
  }
}

export default AddEditDescModal;
