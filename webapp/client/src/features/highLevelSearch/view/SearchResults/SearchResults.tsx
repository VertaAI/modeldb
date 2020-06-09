import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';
import * as R from 'ramda';
import { NavLink, useLocation } from 'react-router-dom';

import DefaultMatchRemoteData from 'shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';
import { IApplicationState } from 'store/store';
import { IPagination } from 'shared/models/Pagination';
import { usePrevious } from 'shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteDataWithReloading';

import { actions, selectors } from '../../store';
import Filters from './Filters/Filters';
import Results from './Results/Results';
import SearchForm from './SearchForm/SearchForm';
import styles from './SearchResults.module.css';
import { parseSearchSettingsFromPathname } from '../../url';
import { paginationSettings } from '../../constants';
import Sorting from './Sorting/Sorting';

const mapStateToProps = (state: IApplicationState) => {
  return {
    entitiesResults: selectors.selectEntitiesResults(state),
    workspaceName: selectCurrentWorkspaceName(state),
    redirectTo: selectors.selectRedirectTo(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadEntities: actions.loadEntities,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const SearchResults = (props: AllProps) => {
  const { loadEntities, entitiesResults, workspaceName, redirectTo } = props;

  const location = useLocation();
  const searchSettings = parseSearchSettingsFromPathname(location);
  const previousSearchSettings = usePrevious(searchSettings);

  React.useEffect(() => {
    // handling of a case when query params are changed by the browser back button or by the header search input
    const isEntitiesLoading = Object.values(entitiesResults).some(
      ({ communication: { isRequesting } }) => isRequesting
    );
    if (
      previousSearchSettings &&
      !R.equals(previousSearchSettings, searchSettings) &&
      !isEntitiesLoading
    ) {
      loadEntities({
        loadType: 'activeEntitiesAndUpdateOthers',
        ...searchSettings,
        pageSize: paginationSettings.pageSize,
      });
    }
  }, [previousSearchSettings, searchSettings]);
  React.useEffect(() => {
    loadEntities({
      loadType: 'activeEntitiesAndUpdateOthers',
      ...searchSettings,
      pageSize: paginationSettings.pageSize,
    });
  }, []);

  if (!searchSettings) {
    return null;
  }

  const loadingActiveEntities =
    entitiesResults[searchSettings.type].communication;
  const activeEntities = entitiesResults[searchSettings.type].data;

  return (
    <div className={styles.root}>
      {redirectTo && (
        <NavLink className={styles.goBack} to={redirectTo}>
          &lt; Back
        </NavLink>
      )}
      <div className={styles.title}>Search</div>
      <div className={styles.searchForm}>
        <SearchForm initialValue={searchSettings.nameOrTag} />
      </div>
      <div className={styles.filters}>
        <Filters
          entitiesResults={entitiesResults}
          activeFilter={searchSettings.type}
        />
      </div>
      <div className={styles.resultsContainer}>
        <div className={styles.sorting}>
          <Sorting value={searchSettings.sorting} />
        </div>
        <div className={styles.results}>
          <DefaultMatchRemoteData
            communication={loadingActiveEntities}
            data={activeEntities}
          >
            {loadedActiveEntitiesResults => {
              const pagination: IPagination = {
                ...paginationSettings,
                currentPage: searchSettings.currentPage,
                totalCount: loadedActiveEntitiesResults.totalCount,
              };

              return (
                <Results
                  resultsBySearchFields={
                    loadedActiveEntitiesResults.data || {
                      name: [],
                      tag: [],
                    }
                  }
                  pagination={pagination}
                  searchValue={searchSettings.nameOrTag}
                  workspaceName={workspaceName}
                />
              );
            }}
          </DefaultMatchRemoteData>
        </div>
      </div>
    </div>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SearchResults);
