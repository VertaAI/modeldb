import * as React from 'react';
import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';

import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import { EntityWithDescription } from 'core/shared/models/Description';
import DescriptionManager from 'core/shared/view/domain/BaseDescriptionManager/DescriptionManager';
import { addOrEditDescription } from 'features/descriptionManager/store';

interface ILocalProps {
  entityType: Exclude<EntityWithDescription, 'endpoint'>;
  entityId: string;
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

class ProjectEntityDescriptionManager extends React.Component<AllProps> {
  public render() {
    return (
      <WithCurrentUserActionsAccesses
        entityId={this.props.entityId}
        entityType={this.props.entityType}
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
)(ProjectEntityDescriptionManager);
