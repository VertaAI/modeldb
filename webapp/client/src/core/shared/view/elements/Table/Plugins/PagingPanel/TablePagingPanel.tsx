import { bind } from 'decko';
import * as React from 'react';

import {
  IPagination,
  getPaginationPageCount,
} from 'core/shared/models/Pagination';

import { Pagination } from './Pagination';

interface ILocalProps {
  pagination: IPagination;
  onCurrentPageChange(page: number): void;
}

class TablePagingPanel extends React.Component<ILocalProps> {
  public componentDidMount() {
    // simple class names is needed for testing!
    this.getPaginationPages().forEach(paginationPageElem => {
      paginationPageElem.parentElement!.classList.add('pagination-page');
    });
    this.addClassForActivePaginationButton();
  }

  public componentDidUpdate() {
    this.addClassForActivePaginationButton();
  }

  public render() {
    return (
      <div className="pagination">
        <Pagination
          currentPage={this.props.pagination.currentPage}
          onCurrentPageChange={this.props.onCurrentPageChange}
          classes={{}}
          pageSize={this.props.pagination.pageSize}
          getMessage={(_: any, { from, to, count }: any) => (
            <span>
              {from}-{to} of {count}
            </span>
          )}
          totalPages={getPaginationPageCount(this.props.pagination)}
          totalCount={this.props.pagination.totalCount}
        />
      </div>
    );
  }

  @bind
  private addClassForActivePaginationButton() {
    const activeLabel = this.getPaginationPages().find(
      n => n.textContent === String(this.props.pagination.currentPage + 1)
    );
    if (activeLabel) {
      activeLabel.parentElement!.classList.add('active-pagination-page');
    }
  }

  @bind
  private getPaginationPages() {
    return Array.from(
      document.querySelectorAll('.MuiButton-root .MuiButton-label')
    );
  }
}

export default TablePagingPanel;
