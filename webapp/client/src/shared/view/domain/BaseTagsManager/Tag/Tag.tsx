import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { makeDefaultTagFilter } from 'shared/models/Filters';
import Confirm from 'shared/view/elements/Confirm/Confirm';
import Draggable from 'shared/view/elements/Draggable/Draggable';
import { Icon } from 'shared/view/elements/Icon/Icon';
import tagBlockStyles from 'shared/view/elements/TagBlock/TagBlock.module.css';

import styles from './Tag.module.css';

interface ILocalProps {
  tag: string;
  isUpdating: boolean;
  isRemovable?: boolean;
  isDraggable?: boolean;
  tagWordReplacer?: string;
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
    const {
      tag,
      isRemovable,
      isUpdating,
      isDraggable = true,
      tagWordReplacer,
    } = this.props;
    return (
      <div
        className={cn(styles.root, {
          [styles.updating]: isUpdating,
          [tagBlockStyles.tag]: true,
          [tagBlockStyles.draggable]: isDraggable,
        })}
      >
        <Confirm
          title={`Delete ${tagWordReplacer || 'Tag'}: ${this.props.tag}`}
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
