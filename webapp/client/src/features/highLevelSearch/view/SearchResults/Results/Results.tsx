import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { IPagination } from 'core/shared/models/Pagination';
import NoResultsStub from 'core/shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import { IResult, SearchField } from 'core/shared/models/HighLevelSearch';
import { IWorkspace } from 'core/shared/models/Workspace';

import { actions } from '../../../store';
import styles from './Results.module.css';
import Result from './Result/Result';

interface ILocalProps {
  searchValue: string;
  resultsBySearchFields: Record<SearchField, IResult[]>;
  pagination: IPagination;
  workspaceName: IWorkspace['name'];
}

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      changePagination: actions.changePagination,
    },
    dispatch
  );
};

type AllProps = ILocalProps & ReturnType<typeof mapDispatchToProps>;

const Results = ({
  resultsBySearchFields,
  searchValue,
  pagination,
  workspaceName,
  changePagination,
}: AllProps) => {
  const results = resultsBySearchFields.tag.concat(resultsBySearchFields.name);
  return (
    <div className={styles.root}>
      {results.length > 0 ? (
        <>
          <div className={styles.items}>
            {results.map(result => (
              <div className={styles.result} key={result.id}>
                <Result
                  result={result}
                  searchValue={searchValue}
                  workspaceName={workspaceName}
                />
              </div>
            ))}
          </div>
        </>
      ) : (
        <NoResultsStub />
      )}
      {results.length > 0 || pagination.currentPage > 0 ? (
        <div className={styles.pagination}>
          <Pagination
            pagination={pagination}
            onCurrentPageChange={changePagination}
          />
        </div>
      ) : null}
    </div>
  );
};

export default connect(
  undefined,
  mapDispatchToProps
)(Results);
