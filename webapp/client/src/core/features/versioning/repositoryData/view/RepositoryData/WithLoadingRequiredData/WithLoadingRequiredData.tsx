import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import {
  selectors,
  actions,
} from 'core/features/versioning/repositoryData/store';
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
  ReturnType<typeof mapDispatchToProps> & { repository: IRepository };

interface ILoadedData {
  tags: CommitTag[];
  branches: Branch[];
}

export default function withLoadingRequiredData<Props extends ILoadedData>(
  WrappedComponent: React.ComponentType<Props>
): React.ComponentType<Omit<Props, keyof ILoadedData>> {
  class Wrapper extends React.Component<
    Omit<Props, keyof ILoadedData> & WrapperOwnProps
  > {
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
        ...wrappedComponentProps
      } = this.props;

      if (
        loadingBranches.isSuccess &&
        branches &&
        loadingTags.isSuccess &&
        tags
      ) {
        const requiredData: ILoadedData = {
          branches,
          tags,
        };
        const props: Props = {
          ...(wrappedComponentProps as any),
          ...requiredData,
        };
        return <WrappedComponent {...props} />;
      }

      return (
        <DefaultMatchRemoteData communication={loadingTags} data={tags}>
          {loadedTags => (
            <DefaultMatchRemoteData
              communication={loadingBranches}
              data={branches}
            >
              {loadedBranches => {
                const requiredData: ILoadedData = {
                  branches: loadedBranches,
                  tags: loadedTags,
                };
                const props: Props = {
                  ...(wrappedComponentProps as any),
                  ...requiredData,
                };
                return <WrappedComponent {...props} />;
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
  )(Wrapper as any);
}
