import * as React from 'react';
import { useLocation } from 'react-router';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import { IRepository } from 'core/shared/models/Versioning/Repository';

import parseCommitsHistorySettings from '../../helpers/parseCommitsHistorySettings';
import styles from './CommitsHistory.module.css';
import CommitsHistoryView from './CommitsHistoryView/CommitsHistoryView';
import { commitsHistory } from '../../store';

interface ILocalProps {
  repository: IRepository;
}

type AllProps = ILocalProps;

const CommitsHistory = ({ repository }: AllProps) => {
  const location = useLocation();
  const settings = parseCommitsHistorySettings(location);

  const {
    communication: loadingCommits,
    data: commits,
  } = commitsHistory.useCommitsHistoryQuery({
    repositoryId: repository.id,
    branch: settings.branch,
  });

  return (
    <div className={styles.root}>
      <DefaultMatchRemoteData communication={loadingCommits} data={commits}>
        {loadedCommits => (
          <CommitsHistoryView
            repository={repository}
            commits={loadedCommits}
            settings={settings}
          />
        )}
      </DefaultMatchRemoteData>
    </div>
  );
};

export default CommitsHistory;
