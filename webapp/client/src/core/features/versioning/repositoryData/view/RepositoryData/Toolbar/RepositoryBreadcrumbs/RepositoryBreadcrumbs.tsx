import React from 'react';
import { connect } from 'react-redux';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IFullCommitComponentLocationComponents } from 'core/shared/models/Versioning/RepositoryData';
import Breadcrumbs, {
  generateBreadcrumbs,
} from 'core/shared/view/domain/Versioning/Breadcrumbs/Breadcrumbs';
import routes from 'core/shared/routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspaceName: selectCurrentWorkspaceName(state),
});

interface ILocalProps {
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  repositoryName: IRepository['name'];
}

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const RepositoryBreadcrumbs: React.FC<AllProps> = ({
  repositoryName,
  fullCommitComponentLocationComponents: { commitPointer, location },
  currentWorkspaceName,
}) => {
  const breadcrumbs = generateBreadcrumbs(
    location,
    (name, locationPathname) => ({
      to: routes.repositoryDataWithLocation.getRedirectPath({
        commitPointer,
        location: locationPathname,
        repositoryName,
        type: 'folder',
        workspaceName: currentWorkspaceName,
      }),
      name,
    }),
    {
      to: routes.repositoryDataWithLocation.getRedirectPath({
        commitPointer,
        repositoryName,
        location: CommitComponentLocation.makeRoot(),
        type: 'folder',
        workspaceName: currentWorkspaceName,
      }),
      name: repositoryName,
    }
  );

  return <Breadcrumbs breadcrumbItems={breadcrumbs} />;
};

export default connect(mapStateToProps)(React.memo(RepositoryBreadcrumbs));
