import * as React from 'react';

import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import * as Common from 'shared/models/Common';

import ConnectedTagsManager from '../ConnectedTagsManager/ConnectedTagsManager';

interface ILocalProps {
  id: string;
  datasetId: string;
  tags: string[];
  entityType: Extract<Common.EntityType, 'dataset' | 'datasetVersion'>;
  isDraggableTags: boolean;
  onClick?(
    e: React.MouseEvent<Element, MouseEvent>,
    byEmptiness: boolean
  ): void;
}

class DatasetEntityTagsManager extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <WithCurrentUserActionsAccesses
        entityType="dataset"
        entityId={this.props.datasetId}
        actions={['update']}
      >
        {({ isLoading, actionsAccesses }) => (
          <ConnectedTagsManager
            id={this.props.id}
            isDraggableTags={this.props.isDraggableTags}
            entityType={this.props.entityType}
            tags={this.props.tags}
            isLoadingUserAccess={isLoading}
            isReadOnly={!actionsAccesses.update}
            onClick={this.props.onClick}
          />
        )}
      </WithCurrentUserActionsAccesses>
    );
  }
}

export default DatasetEntityTagsManager;
