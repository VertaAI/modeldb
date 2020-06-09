import React from 'react';
import { connect } from 'react-redux';

import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'shared/models/Versioning/Repository';
import { IFullCommitComponentLocationComponents } from 'shared/models/Versioning/RepositoryData';
import Breadcrumbs, {
  generateBreadcrumbs,
} from 'shared/view/domain/Versioning/Breadcrumbs/Breadcrumbs';
import routes from 'shared/routes';
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
