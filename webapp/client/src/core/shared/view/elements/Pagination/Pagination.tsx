import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import PaginationLib from 'react-paginate';

import {
  IPagination,
  getPaginationPageCount,
} from 'core/shared/models/Pagination';

import styles from './Pagination.module.css';

interface ILocalProps {
  pagination: IPagination;
  onCurrentPageChange(currentPage: number): void;
}

class Pagination extends React.PureComponent<ILocalProps> {
  public render() {
    const { pagination } = this.props;
    const pageCount = getPaginationPageCount(pagination);
    return (
      <PaginationLib
        pageCount={pageCount}
        pageRangeDisplayed={5}
        marginPagesDisplayed={2}
        forcePage={pagination.currentPage}
        onPageChange={this.onCurrentPageChange}
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
  }

  @bind
  private onCurrentPageChange({ selected }: any) {
    this.props.onCurrentPageChange(selected);
  }
}

export default Pagination;
