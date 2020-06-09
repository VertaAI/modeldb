import * as React from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  RepositoryBranches,
  CommitTag,
} from 'core/shared/models/Versioning/RepositoryData';
import BranchesAndTagsList from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/BranchesAndTagsList';
import routes from 'core/shared/routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

interface ILocalProps {
  repository: IRepository;
  branches: RepositoryBranches;
  tags: CommitTag[];
  commitPointer: CommitPointer;
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    currentWorkspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const BranchesAndTagsListContainer = ({
  branches,
  tags,
  commitPointer,
  repository,
  currentWorkspaceName,
}: AllProps) => {
  const history = useHistory();

  const onChangeCommitPointer = (newCommitPointer: CommitPointer) => {
    history.push(
      routes.repositoryDataWithLocation.getRedirectPath({
        workspaceName: currentWorkspaceName,
        commitPointer: newCommitPointer,
        location: CommitComponentLocation.makeRoot(),
        repositoryName: repository.name,
        type: 'folder',
      })
    );
  };

  return (
    <BranchesAndTagsList
      branches={branches}
      tags={tags}
      commitPointer={commitPointer}
      dataTest="branches-and-tags"
      onChangeCommitPointer={onChangeCommitPointer}
    />
  );
};

export default connect(mapStateToProps)(BranchesAndTagsListContainer);
