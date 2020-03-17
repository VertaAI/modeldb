import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import {
  selectors,
  actions,
} from 'core/features/repository/repositoryData/store';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitTag,
  Branch,
} from 'core/shared/models/Versioning/RepositoryData';
import { IApplicationState } from 'store/store';

const mapStateToProps = (state: IApplicationState) => {
  return {
    tags: selectors.selectTags(state),
    loadingTags: selectors.selectCommunications(state).loadingTags,

    branches: selectors.selectBranches(state),
    loadingBranches: selectors.selectCommunications(state).loadingBranches,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadTags: actions.loadTags,
      loadBranches: actions.loadBranches,
    },
    dispatch
  );
};

type WrapperOwnProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

export default function withLoadingRequiredData<
  Props extends {
    repository: IRepository;
    tags: CommitTag[];
    branches: Branch[];
  }
>(
  WrappedComponent: React.ComponentType<Props>
): React.ComponentType<Omit<Props, 'tags' | 'branches'>> {
  class Wrapper extends React.Component<Props & WrapperOwnProps> {
    public componentDidMount() {
      this.props.loadTags({
        repositoryId: this.props.repository.id,
      });
      this.props.loadBranches({
        repositoryId: this.props.repository.id,
      });
    }

    public render() {
      const {
        loadTags,
        loadingTags,
        tags,
        loadingBranches,
        branches,
        loadBranches,
        ...restProps
      } = this.props;

      if (
        loadingBranches.isSuccess &&
        branches &&
        loadingTags.isSuccess &&
        tags
      ) {
        return (
          <WrappedComponent
            {...restProps as any}
            tags={tags}
            branches={branches}
          />
        );
      }

      return (
        <DefaultMatchRemoteData communication={loadingTags} data={tags}>
          {loadedTags => (
            <DefaultMatchRemoteData
              communication={loadingBranches}
              data={branches}
            >
              {loadedBranches => {
                return (
                  <WrappedComponent
                    {...restProps as any}
                    tags={loadedTags}
                    branches={loadedBranches}
                  />
                );
              }}
            </DefaultMatchRemoteData>
          )}
        </DefaultMatchRemoteData>
      );
    }
  }
  return connect(
    mapStateToProps,
    mapDispatchToProps
  )(Wrapper as any) as any;
}
