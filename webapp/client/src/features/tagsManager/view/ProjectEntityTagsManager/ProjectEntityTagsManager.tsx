import * as React from 'react';

import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import * as Common from 'core/shared/models/Common';

import ConnectedTagsManager from '../ConnectedTagsManager/ConnectedTagsManager';

interface ILocalProps {
  id: string;
  projectId: string;
  tags: string[];
  entityType: Extract<
    Common.EntityType,
    'project' | 'experimentRun' | 'experiment'
  >;
  isDraggableTags: boolean;
  onClick?(
    e: React.MouseEvent<Element, MouseEvent>,
    byEmptiness: boolean
  ): void;
}

class ProjectEntityTagsManager extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <WithCurrentUserActionsAccesses
        entityType={this.props.entityType}
        entityId={this.props.id}
        actions={['update']}
      >
        {({ actionsAccesses, isLoading }) => (
          <ConnectedTagsManager
            id={this.props.id}
            isDraggableTags={this.props.isDraggableTags}
            tags={this.props.tags}
            entityType={this.props.entityType}
            isLoadingUserAccess={isLoading}
            isReadOnly={!actionsAccesses.update}
            onClick={this.props.onClick}
          />
        )}
      </WithCurrentUserActionsAccesses>
    );
  }
}

export default ProjectEntityTagsManager;
