import * as React from 'react';

import * as Common from 'core/shared/models/Common';

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
      <ConnectedTagsManager
        id={this.props.id}
        isDraggableTags={this.props.isDraggableTags}
        entityType={this.props.entityType}
        tags={this.props.tags}
        isLoadingUserAccess={false}
        isReadOnly={false}
        onClick={this.props.onClick}
      />
    );
  }
}

export default DatasetEntityTagsManager;
