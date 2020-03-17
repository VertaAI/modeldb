import * as React from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import {
  selectors,
  actions,
} from 'core/features/repository/repositoryData/store';
import * as DataLocation from 'core/shared/models/Repository/DataLocation';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { CommitPointer } from 'core/shared/models/Repository/RepositoryData';
import BranchesAndTagsList from 'core/shared/view/domain/Repository/RepositoryData/BranchesAndTagsList/BranchesAndTagsList';
import { IApplicationState } from 'store/store';

import * as routeHelpers from '../../routeHelpers';

interface ILocalProps {
  repository: IRepository;
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    tags: selectors.selectTags(state)!,
    branches: selectors.selectBranches(state)!,

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
}: AllProps) => {
  const history = useHistory();

  const onChangeCommitPointer = (newCommitPointer: CommitPointer) => {
    changeCommitPointer(newCommitPointer);
    history.push(
      routeHelpers.getRedirectPath({
        commitPointer: newCommitPointer,
        location: DataLocation.makeRoot(),
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
      onChangeCommitPointer={onChangeCommitPointer}
    />
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(BranchesAndTagsListContainer);
