import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { EntityType } from 'core/shared/models/Common';
import { IApplicationState } from 'store/store';
import { addTag, removeTag, selectIsUpdatingTags } from 'store/tagAction';

import TagsManager from 'core/shared/view/domain/TagsManager/TagsManager';

interface ILocalProps {
  id: string;
  isLoadingUserAccess: boolean;
  isReadOnly: boolean;
  tags: string[];
  entityType: EntityType;
  isDraggableTags: boolean;
  onClick?(e: React.MouseEvent<Element, MouseEvent>, byEmptiess: boolean): void;
}

interface IPropsFromState {
  isUpdating: boolean;
}

interface IActionProps {
  addTag: typeof addTag;
  removeTag: typeof removeTag;
}

interface ILocalState {
  isShowAddTagActionModal: boolean;
}

type AllProps = ILocalProps & IActionProps & IPropsFromState;

class ConnectedTagManager extends React.Component<AllProps, ILocalState> {
  public render() {
    const {
      tags,
      isDraggableTags,
      isLoadingUserAccess,
      isUpdating,
      isReadOnly,
      onClick,
    } = this.props;

    const isAvailableEditing = !isLoadingUserAccess && !isReadOnly;

    return (
      <TagsManager
        tags={tags}
        isUpdating={isUpdating}
        isShowPlaceholder={tags.length < 1 && !isReadOnly}
        isDraggableTags={isDraggableTags}
        isRemovableTags={isAvailableEditing}
        isAvailableTagAdding={isAvailableEditing}
        onRemoveTag={this.removeTag}
        onAddTag={this.addTag}
        onClick={onClick}
      />
    );
  }

  @bind
  private addTag(tag: string) {
    this.props.addTag(this.props.id, [tag], this.props.entityType);
    this.setState({ isShowAddTagActionModal: false });
  }

  @bind
  private removeTag(tag: string) {
    const { id, entityType } = this.props;
    this.props.removeTag(id, [tag], entityType, false);
  }
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators({ addTag, removeTag }, dispatch);

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    isUpdating: selectIsUpdatingTags(state),
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ConnectedTagManager);
