import * as React from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import {
  selectors,
  actions,
} from 'core/features/versioning/repositoryData/store';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { CommitPointer } from 'core/shared/models/Versioning/RepositoryData';
import BranchesAndTagsList from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/BranchesAndTagsList';
import routes from 'routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'store/workspaces';

interface ILocalProps {
  repository: IRepository;
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    tags: selectors.selectTags(state)!,
    branches: selectors.selectBranches(state)!,
    currentWorkspaceName: selectCurrentWorkspaceName(state),
    commitPointer: selectors.selectCommitPointer(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      changeCommitPointer: actions.changeCommitPointer,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const BranchesAndTagsListContainer = ({
  branches,
  tags,
  commitPointer,
  repository,
  changeCommitPointer,
  currentWorkspaceName,
}: AllProps) => {
  const history = useHistory();

  const onChangeCommitPointer = (newCommitPointer: CommitPointer) => {
    changeCommitPointer(newCommitPointer);
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

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(BranchesAndTagsListContainer);
