import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import { IFilterContext } from 'features/filter';
import {
  getDefaultDatasetVersionsOptions,
  loadDatasetVersions,
  resetDatasetVersionsPagination,
  DatasetVersions,
} from 'features/datasetVersions';
import { defaultQuickFilters } from 'core/shared/models/Filters';
import routes, { GetRouteParams } from 'core/shared/routes';

import DatasetDetailsLayout from '../shared/DatasetDetailsLayout/DatasetDetailsLayout';

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadDatasetVersions,
      resetDatasetVersionsPagination,
      getDefaultDatasetVersionsOptions,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapDispatchToProps> &
  RouteComponentProps<GetRouteParams<typeof routes.datasetVersions>>;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class DatasetVersionsPage extends React.PureComponent<AllProps> {
  public state: ILocalState = {
    isNeedResetPagination: false,
  };

  private filterContext: IFilterContext;

  constructor(props: AllProps) {
    super(props);
    const contextName = `datasetVersions-${this.props.match.params.datasetId}`;
    this.filterContext = {
      quickFilters: [defaultQuickFilters.tag],
      name: contextName,
      onApplyFilters: filters => {
        if (this.state.isNeedResetPagination) {
          this.props.resetDatasetVersionsPagination();
        }
        this.props.loadDatasetVersions(
          this.props.match.params.datasetId,
          filters
        );
        if (!this.state.isNeedResetPagination) {
          this.setState({ isNeedResetPagination: true });
        }
      },
    };
    this.props.getDefaultDatasetVersionsOptions();
  }

  public render() {
    const {
      match: {
        params: { datasetId },
      },
    } = this.props;

    return (
      <DatasetDetailsLayout
        filterBarSettings={{
          context: this.filterContext,
          title: 'Filter Versions',
        }}
      >
        <DatasetVersions datasetId={datasetId} />
      </DatasetDetailsLayout>
    );
  }
}

export default connect(
  null,
  mapDispatchToProps
)(DatasetVersionsPage);
