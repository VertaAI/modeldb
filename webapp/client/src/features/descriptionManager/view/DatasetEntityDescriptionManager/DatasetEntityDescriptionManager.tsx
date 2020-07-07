import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import { EntityWithDescription } from 'shared/models/Description';
import DescriptionManager from 'shared/view/domain/BaseDescriptionManager/DescriptionManager';

import { addOrEditDescription } from 'features/descriptionManager/store';

interface ILocalProps {
  entityType: Extract<EntityWithDescription, 'dataset' | 'datasetVersion'>;
  entityId: string;
  datasetId: string;
  description: string;
}

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      addOrEditDescription,
    },
    dispatch
  );
};

type AllProps = ILocalProps & ReturnType<typeof mapDispatchToProps>;

class DatasetEntityDescriptionManager extends React.PureComponent<AllProps> {
  public render() {
    return (
      <WithCurrentUserActionsAccesses
        entityType={'dataset'}
        entityId={this.props.datasetId}
        actions={['update']}
      >
        {({ isLoading, actionsAccesses }) => (
          <DescriptionManager
            description={this.props.description}
            entityType={this.props.entityType}
            entityId={this.props.entityId}
            isLoadingAccess={isLoading}
            isReadOnly={!actionsAccesses.update}
            onAddOrEditDescription={this.props.addOrEditDescription}
          />
        )}
      </WithCurrentUserActionsAccesses>
    );
  }
}

export default connect(
  undefined,
  mapDispatchToProps
)(DatasetEntityDescriptionManager);
