import { bind } from 'decko';
import * as React from 'react';

import Button from 'core/shared/view/elements/Button/Button';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';

import styles from './AddTagModal.module.css';

interface ILocalProps {
  isOpen: boolean;
  onAdd(tag: string): void;
  onClose(): void;
}

interface ILocalState {
  tag: string;
  tagLimitReached: boolean;
}

class AddTagModal extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = { tag: '', tagLimitReached: false };

  public render() {
    const { isOpen, onClose } = this.props;

    return (
      <Popup
        title={'Add Tag'}
        titleIcon="rounded-plus-filled"
        contentLabel="add-tag-action"
        isOpen={isOpen}
        onRequestClose={onClose}
      >
        <div className={styles.root} data-test="tags-manager-creator">
          <div className={styles.message}>
            <input
              type="text"
              name="tag-submitted"
              placeholder="your tag"
              id="tagInput"
              className={styles.input}
              onChange={this.updateInput}
              onKeyUp={this.onAddTagActionViaEnter}
              autoFocus={true}
              maxLength={40}
              data-test="tags-manager-creator-input"
            />
            {this.state.tagLimitReached && (
              <div className={styles.prompt_tag_limit}>
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
                onClick={this.onAddTag}
                dataTest="tags-manager-create-button"
              >
                Add
              </Button>
            </div>
          </div>
        </div>
      </Popup>
    );
  }

  @bind
  private updateInput(event: React.ChangeEvent<HTMLInputElement>) {
    if (event.target.value.length === 40) {
      this.setState({ tagLimitReached: true });
    } else {
      this.setState({ tagLimitReached: false });
    }
    this.setState({ tag: event.target.value });
  }

  @bind
  private onAddTagActionViaEnter(event: React.KeyboardEvent) {
    event.preventDefault();
    if (event.keyCode === 13) {
      this.onAddTag();
    }
  }

  @bind
  private onAddTag() {
    this.props.onAdd(this.state.tag);
    this.props.onClose();
  }
}

export default AddTagModal;
