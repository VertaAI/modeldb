import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { makeDefaultTagFilter } from 'core/features/filter/Model';
import Confirm from 'core/shared/view/elements/Confirm/Confirm';
import Draggable from 'core/shared/view/elements/Draggable/Draggable';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import tagBlockStyles from 'core/shared/view/elements/TagBlock/TagBlock.module.css';

import styles from './Tag.module.css';

interface ILocalProps {
  tag: string;
  isUpdating: boolean;
  isRemovable?: boolean;
  isDraggable?: boolean;
  onRemove?(tag: string): void;
}

interface ILocalState {
  isShowDeleteTagModal: boolean;
}

class Tag extends React.Component<ILocalProps> {
  public state: ILocalState = {
    isShowDeleteTagModal: false,
  };

  public render() {
    const { tag, isRemovable, isUpdating, isDraggable = true } = this.props;
    return (
      <div
        className={cn(
          styles.root,
          { [styles.updating]: isUpdating },
          isDraggable ? tagBlockStyles.tag_draggable : tagBlockStyles.tag_static
        )}
      >
        <Confirm
          title={`Delete Tag: ${this.props.tag}`}
          titleIcon="trash"
          isOpen={this.state.isShowDeleteTagModal}
          onCancel={this.handleCloseModal}
          onConfirm={this.handleCloseModalAndDelete}
        >
          Are you sure?
        </Confirm>
        <span data-test="tags-manager-tag">
          {isDraggable ? (
            <Draggable
              type="Filter"
              data={makeDefaultTagFilter(tag)}
              additionalClassName={styles.projectTags}
            >
              <span draggable={true} title={tag}>
                {tag.length > 24 ? tag.substr(0, 23) + '..' : tag}
              </span>
            </Draggable>
          ) : (
            <span>{tag}</span>
          )}
        </span>
        {isRemovable && (
          <div className={styles.tag_action}>
            <div
              ref={tag}
              onClick={this.showAddTagModal}
              data-test="tags-manager-open-tag-deletion-confirm"
            >
              <Icon className={styles.action_icon} type="close" />
            </div>
          </div>
        )}
      </div>
    );
  }

  @bind
  private handleCloseModal() {
    this.setState({ isShowDeleteTagModal: false });
  }

  @bind
  private handleCloseModalAndDelete() {
    this.props.onRemove && this.props.onRemove(this.props.tag);
    this.setState({ isShowDeleteTagModal: false });
  }

  @bind
  private showAddTagModal() {
    this.setState({ isShowDeleteTagModal: true });
  }
}

export default Tag;
