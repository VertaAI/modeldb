import { bind } from 'decko';
import * as React from 'react';

import AddTagModal from './AddTagModal/AddTagModal';
import Tag from './Tag/Tag';

import styles from './TagsManager.module.css';
import ActionIcon from '../../elements/ActionIcon/ActionIcon';

interface ILocalProps {
  tags: string[];
  isShowPlaceholder: boolean;
  isUpdating: boolean;
  isDraggableTags: boolean;
  isRemovableTags: boolean;
  isAvailableTagAdding: boolean;
  onAddTag(tag: string): void;
  onRemoveTag(tag: string): void;
  onClick?(e: React.MouseEvent<Element, MouseEvent>, byEmptiess: boolean): void;
}

interface ILocalState {
  isShowAddTagActionModal: boolean;
}

class TagManager extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isShowAddTagActionModal: false,
  };

  public render() {
    const {
      tags,
      isUpdating,
      isDraggableTags,
      isRemovableTags,
      isAvailableTagAdding,
      isShowPlaceholder,
      onRemoveTag,
    } = this.props;
    const { isShowAddTagActionModal } = this.state;

    return (
      <div
        className={tags.length > 0 ? styles.root : styles.root_empty}
        onClick={this.onClickByEmptiness}
        data-test="tags-manager"
      >
        {isShowPlaceholder && (
          <div className={styles.tag_sub_heading_div}>
            <p className={styles.tag_sub_heading}>Tags</p>
          </div>
        )}
        {tags.map((tag: string, i: number) => (
          <Tag
            tag={tag}
            isDraggable={isDraggableTags}
            isRemovable={isRemovableTags}
            isUpdating={isUpdating}
            onRemove={onRemoveTag}
            key={i}
          />
        ))}
        {isAvailableTagAdding && (
          <div className={styles.open_add_tag_modal_wrapper}>
            <ActionIcon
              className={styles.open_add_tag_icon}
              iconType="rounded-plus-filled"
              dataTest="tags-manager-open-tag-creator-button"
              onClick={this.showAddTagModal}
            />
            {isShowAddTagActionModal && (
              <AddTagModal
                isOpen={isShowAddTagActionModal}
                onAdd={this.onAddTag}
                onClose={this.closeAddTagModal}
              />
            )}
          </div>
        )}
      </div>
    );
  }

  @bind
  private onClickByEmptiness(e: React.MouseEvent<Element, MouseEvent>) {
    if (this.props.onClick) {
      this.props.onClick(
        e,
        [styles.root, styles.root_empty].some(rootClass =>
          (e.target as Element).classList.contains(rootClass)
        )
      );
    }
  }

  @bind
  private showAddTagModal() {
    this.setState({ isShowAddTagActionModal: true });
  }
  @bind
  private closeAddTagModal() {
    this.setState({ isShowAddTagActionModal: false });
  }

  @bind
  private onAddTag(tag: string) {
    this.props.onAddTag(tag);
    this.setState({ isShowAddTagActionModal: false });
  }
}

export default TagManager;
