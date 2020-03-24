import React, { useCallback } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import {
  actions,
  selectors,
} from 'core/features/versioning/repositories/store';
import { IRepository, Label } from 'core/shared/models/Versioning/Repository';
import TagsManager from 'core/shared/view/domain/TagsManager/TagsManager';
import { IApplicationState } from 'store/store';

import styles from './RepositoryLabelsManager.module.css';

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
) => ({
  addingRepositoryLabel: selectors.selectAddingRepositoryLabelCommunication(
    state,
    localProps.repository.id
  ),
  deletingRepositoryLabel: selectors.selectDeletingRepositoryLabelCommunication(
    state,
    localProps.repository.id
  ),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      deleteRepositoryLabel: actions.deleteRepositoryLabel,
      addRepositoryLabel: actions.addRepositoryLabel,
    },
    dispatch
  );
};

interface ILocalProps {
  repository: IRepository;
}

type AllProps = ILocalProps &
  ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps>;

const RepositoryLabelsManager: React.FC<AllProps> = ({
  deleteRepositoryLabel,
  addRepositoryLabel,
  repository,
  addingRepositoryLabel,
  deletingRepositoryLabel,
}) => {
  const onAddLabel = useCallback(
    (label: Label) =>
      addRepositoryLabel({ repositoryId: repository.id, label }),
    [repository.id, addRepositoryLabel]
  );

  const onDeleteLabel = useCallback(
    (label: Label) =>
      deleteRepositoryLabel({ repositoryId: repository.id, label }),
    [repository.id, deleteRepositoryLabel]
  );

  return (
    <div className={styles.root}>
      <TagsManager
        tags={repository.labels}
        isAvailableTagAdding={true}
        isDraggableTags={false}
        isRemovableTags={true}
        isShowPlaceholder={repository.labels.length === 0}
        isUpdating={
          addingRepositoryLabel.isRequesting ||
          deletingRepositoryLabel.isRequesting
        }
        onAddTag={onAddLabel}
        onRemoveTag={onDeleteLabel}
        tagWordReplacer="Label"
      />
    </div>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RepositoryLabelsManager);
