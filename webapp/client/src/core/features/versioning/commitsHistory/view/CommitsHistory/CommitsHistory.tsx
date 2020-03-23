import * as React from 'react';
import { connect } from 'react-redux';
import { useLocation } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IApplicationState } from 'store/store';

import { paginationPageSize } from '../../constants';
import parseCommitsHistorySettings from '../../helpers/parseCommitsHistorySettings';
import { actions, selectors } from '../../store';
import styles from './CommitsHistory.module.css';
import CommitsHistoryView from './CommitsHistoryView/CommitsHistoryView';

interface ILocalProps {
  repository: IRepository;
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    commitsWithPagination: selectors.selectCommitsWithPagination(state),
    loadingCommits: selectors.selectCommunications(state).loadingCommits,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommits: actions.loadCommits,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const CommitsHistory = ({
  repository,
  loadCommits,
  loadingCommits,
  commitsWithPagination,
}: AllProps) => {
  const location = useLocation();
  const settings = parseCommitsHistorySettings(location);

  React.useEffect(() => {
    loadCommits({
      repositoryId: repository.id,
      branch: settings.branch,
      location: settings.location,
      paginationSettings: {
        currentPage: settings.currentPage,
        pageSize: paginationPageSize,
      },
    });
  }, [location.pathname, location.search]);

  return (
    <div className={styles.root}>
      <DefaultMatchRemoteData
        communication={loadingCommits}
        data={commitsWithPagination}
      >
        {loadedCommitsWithPagination => (
          <CommitsHistoryView
            repository={repository}
            commitsWithPagination={loadedCommitsWithPagination}
            settings={settings}
          />
        )}
      </DefaultMatchRemoteData>
    </div>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CommitsHistory);
