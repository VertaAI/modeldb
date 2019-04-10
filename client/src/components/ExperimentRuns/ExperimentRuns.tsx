import { GridReadyEvent } from 'ag-grid-community';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-material.css';
import { AgGridReact } from 'ag-grid-react';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import Preloader from 'components/shared/Preloader/Preloader';
import ModelRecord from 'models/ModelRecord';
import routes, { GetRouteParams } from 'routes';
import {
  IColumnConfig,
  IColumnMetaData,
  selectColumnConfig,
} from 'store/dashboard-config';
import {
  selectExperimentRuns,
  selectIsLoadingExperimentRuns,
} from 'store/experiment-runs';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import {
  defaultColDefinitions,
  returnColumnDefs,
} from './columnDefinitions/Definitions';
import DashboardConfig from './DashboardConfig/DashboardConfig';
import styles from './ExperimentRuns.module.css';

type IUrlProps = GetRouteParams<typeof routes.expirementRuns>;

interface IPropsFromState {
  data: ModelRecord[] | null;
  loading: boolean;
  defaultColDefinitions: any;
  filterState: { [index: string]: {} };
  filtered: boolean;
  columnConfig: IColumnConfig;
}

interface IOperator {
  '>': any;
  '<': any;
  [key: string]: any;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;

class ExperimentRuns extends React.Component<AllProps> {
  private gridApi: any;
  private columnApi: any;
  private data: any;

  public componentWillReceiveProps(nextProps: AllProps) {
    if (this.gridApi !== undefined) {
      setTimeout(this.callFilterUpdate, 100);
    }
    if (this.gridApi && this.props.columnConfig !== nextProps.columnConfig) {
      this.gridApi.setColumnDefs(returnColumnDefs(nextProps.columnConfig));
      const el = document.getElementsByClassName('ag-center-cols-viewport');
      if (el !== undefined && el[0] !== undefined) {
        el[0].scrollLeft += 200;
      }
    }
  }

  public render() {
    const { data, loading, columnConfig } = this.props;
    return loading ? (
      <Preloader variant="dots" />
    ) : data ? (
      <React.Fragment>
        <DashboardConfig />
        <div className={`ag-theme-material ${styles.aggrid_wrapper}`}>
          <AgGridReact
            pagination={true}
            onGridReady={this.onGridReady}
            animateRows={true}
            columnDefs={returnColumnDefs(columnConfig)}
            rowData={undefined}
            domLayout="autoHeight"
            defaultColDef={this.props.defaultColDefinitions}
            isExternalFilterPresent={this.isExternalFilterPresent}
            doesExternalFilterPass={this.doesExternalFilterPass}
          />
        </div>
      </React.Fragment>
    ) : (
      ''
    );
  }

  @bind
  private callFilterUpdate() {
    this.gridApi.onFilterChanged();
  }

  @bind
  private onGridReady(event: GridReadyEvent) {
    this.gridApi = event.api;
    this.columnApi = event.columnApi;
    this.gridApi.setRowData(this.props.data);
  }

  @bind
  private isExternalFilterPresent() {
    return this.props.filtered;
  }

  @bind
  private funEvaluate(filter: any) {
    // this.data is from the bind(node) where node is table row data
    // **ts forced creation of public data var to be able access node
    const operators: IOperator = {
      '<': (a: number, b: number) => a < b,
      '>': (a: number, b: number) => a > b,
    };
    switch (filter.type) {
      case 'tag':
        return this.data.tags.includes(filter.key);
      case 'param':
        return this.data[filter.subtype].find(
          (params: any) => params.key === filter.param
        )
          ? operators[filter.operator](
              Number(
                this.data[filter.subtype].find((params: any) => {
                  if (params.key === filter.param) {
                    return params.value;
                  }
                }).value
              ),
              Number(filter.value)
            )
          : false;
      default:
        return true;
    }
  }

  @bind
  private doesExternalFilterPass(node: any) {
    return Object.values(this.props.filterState)
      .map(this.funEvaluate.bind(node))
      .every(val => val === true);
  }
}

// filterState and filtered should be provided by from IApplicationState -> customFilter
const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  defaultColDefinitions,
  columnConfig: selectColumnConfig(state),
  data: selectExperimentRuns(state),
  loading: selectIsLoadingExperimentRuns(state),
  filterState: {},
  filtered: false,
});

export default connect(mapStateToProps)(ExperimentRuns);
