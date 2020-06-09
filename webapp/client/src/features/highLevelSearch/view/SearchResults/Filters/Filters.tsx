import cn from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import {
  Entities,
  ActiveFilter,
  IEntitiesResults,
} from 'shared/models/HighLevelSearch';
import matchType from 'shared/utils/matchType';

import { actions } from '../../../store';
import styles from './Filters.module.css';
import { IApplicationState } from 'store/store';

interface ILocalProps {
  entitiesResults: IEntitiesResults;
  activeFilter: ActiveFilter;
}

const mapStateToProps = (state: IApplicationState) => ({
  isEnableRepositories: true,
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      setFilter: actions.setFilter,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps>;

const Filters = ({
  entitiesResults,
  activeFilter,
  setFilter,
  isEnableRepositories,
}: AllProps) => {
  const filteredEntities = Object.values(Entities).filter(type =>
    matchType(
      {
        projects: () => true,
        experiments: () => true,
        experimentRuns: () => true,
        datasets: () => true,
        repositories: () => isEnableRepositories,
      },
      type
    )
  );

  return (
    <div className={styles.root}>
      {filteredEntities.map(type => (
        <div
          className={cn(styles.filter, {
            [styles.active]: type === activeFilter,
          })}
          key={type}
          onClick={type !== activeFilter ? () => setFilter(type) : undefined}
        >
          <span className={styles.filter__name}>
            {matchType(
              {
                projects: () => 'Projects',
                experiments: () => 'Experiments',
                experimentRuns: () => 'Experiment runs',
                datasets: () => 'Datasets',
                repositories: () => 'Repositories',
              },
              type
            )}
          </span>
          <span className={styles.filter__totalCount}>
            {(() => {
              if (entitiesResults[type].communication.isRequesting) {
                return '...';
              } else {
                return entitiesResults[type].data.totalCount;
              }
            })()}
          </span>
        </div>
      ))}
    </div>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Filters);
