import cn from 'classnames';
import * as React from 'react';
import PaginationLib from 'react-paginate';
import { useLocation } from 'react-router';

import { IPagination, getPaginationPageCount } from 'shared/models/Pagination';

import styles from './Pagination.module.css';

interface ILocalProps {
  pagination: IPagination;
  onCurrentPageChange(currentPage: number): void;
}

const usePageFromURL = () => {
  const location = useLocation();
  const paramsSearch = new URLSearchParams(location.search);
  const page = paramsSearch.get('page');
  if (page === null) {
    return 0;
  }

  return isNaN(Number(page)) ? 0 : Number(page) - 1;
};

const Pagination: React.FC<ILocalProps> = ({
  pagination,
  onCurrentPageChange,
}) => {
  const pageCount = getPaginationPageCount(pagination);
  const pageFromURL = usePageFromURL();

  const onPageChange = React.useCallback(
    ({ selected }: { selected: number }) => onCurrentPageChange(selected),
    [onCurrentPageChange]
  );

  React.useEffect(() => {
    if (pageFromURL !== pagination.currentPage) {
      onCurrentPageChange(pageFromURL);
    }
  }, [pageFromURL]);

  return (
    <PaginationLib
      pageCount={pageCount}
      pageRangeDisplayed={5}
      marginPagesDisplayed={2}
      forcePage={pagination.currentPage}
      onPageChange={onPageChange}
      previousLabel={'<'}
      nextLabel={'>'}
      breakLabel={'...'}
      // simple class names is needed for testing!
      containerClassName={cn(styles.root, 'pagination')}
      activeClassName={styles.active}
      pageClassName={cn(styles.page, 'pagination-page')}
      previousClassName={
        pagination.currentPage === 0 ? styles.disabled : undefined
      }
      nextClassName={
        pageCount - 1 === pagination.currentPage ? styles.disabled : undefined
      }
    />
  );
};

export default Pagination;
