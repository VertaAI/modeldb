import { GridReadyEvent } from 'ag-grid-community';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-material.css';
import { AgGridReact } from 'ag-grid-react';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import { FilterContextPool } from 'models/FilterContextPool';
import { PropertyType } from 'models/Filters';
import ModelRecord from 'models/ModelRecord';
import routes, { GetRouteParams } from 'routes';
import { IColumnMetaData } from 'store/dashboard-config';
import { fetchExperimentRuns } from 'store/experiment-runs';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import loader from 'components/images/loader.gif';
import styles from './ExperimentRuns.module.css';

import { defaultColDefinitions, returnColumnDefs } from './columnDefinitions/Definitions';
import DashboardConfig from './DashboardConfig/DashboardConfig';

let currentProjectID: string;
const locationRegEx = /\/project\/([a-z0-9\-]+)\/exp-runs/im;
FilterContextPool.registerContext({
  getMetadata: () => [{ propertyName: 'Name', type: PropertyType.STRING }, { propertyName: 'Tag', type: PropertyType.STRING }],
  isFilteringSupport: true,
  isValidLocation: (location: string) => {
    if (locationRegEx.test(location)) {
      const match = location.match(locationRegEx);
      currentProjectID = match ? match[1] : '';
      return true;
    } else {
      return false;
    }
  },
  name: 'ModelRecord',
  onApplyFilters: (filters, dispatch) => {
    dispatch(fetchExperimentRuns(currentProjectID, filters));
  },
  onSearch: (text: string, dispatch) => {
    dispatch(
      fetchExperimentRuns(currentProjectID, [
        {
          invert: false,
          name: 'Name',
          type: PropertyType.STRING,
          value: text
        }
      ])
    );
  }
});

type IUrlProps = GetRouteParams<typeof routes.expirementRuns>;

interface IPropsFromState {
  data?: ModelRecord[] | undefined;
  loading: boolean;
  defaultColDefinitions: any;
  filterState: { [index: string]: {} };
  filtered: boolean;
  columnConfig: Map<string, IColumnMetaData>;
}

interface IOperator {
  '>': any;
  '<': any;
  [key: string]: any;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;

class ExperimentRuns extends React.Component<AllProps> {
  public gridApi: any;
  public columnApi: any;
  public data: any;

  public callFilterUpdate = () => {
    this.gridApi.onFilterChanged();
  };

  public componentWillReceiveProps() {
    if (this.gridApi !== undefined) {
      setTimeout(this.callFilterUpdate, 100);
    }
    const updatedConfig = this.props.columnConfig;
    if (this.gridApi && updatedConfig !== undefined) {
      this.gridApi.setColumnDefs(returnColumnDefs(updatedConfig));
      const el = document.getElementsByClassName('ag-center-cols-viewport');
      if (el !== undefined && el[0] !== undefined) {
        el[0].scrollLeft += 200;
      }
    }
  }

  public componentDidUpdate() {
    if (this.props.data !== undefined && this.gridApi !== undefined) {
      this.gridApi.setRowData(this.props.data);
    }
  }
  public render() {
    const { data, loading, columnConfig } = this.props;

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : data ? (
      <div>
        <DashboardConfig />
        <div className={`ag-theme-material ${styles.aggrid_wrapper}`}>
          <AgGridReact
            pagination={true}
            onGridReady={this.onGridReady}
            animateRows={true}
            getRowHeight={this.gridRowHeight}
            columnDefs={returnColumnDefs(columnConfig)}
            rowData={undefined}
            domLayout="autoHeight"
            defaultColDef={this.props.defaultColDefinitions}
            isExternalFilterPresent={this.isExternalFilterPresent}
            doesExternalFilterPass={this.doesExternalFilterPass}
          />
        </div>
      </div>
    ) : (
      ''
    );
  }

  public onGridReady = (event: GridReadyEvent) => {
    this.gridApi = event.api;
    this.columnApi = event.columnApi;
    this.gridApi.setRowData(this.props.data);
  };

  public gridRowHeight = (params: any) => {
    const data = params.node.data;
    if ((data.metrics && data.metrics.length > 3) || (data.hyperparameters && data.hyperparameters.length > 3)) {
      if (data.metrics.length > data.hyperparameters.length) {
        return (data.metric.length - 3) * 5 + 220;
      }
      return data.hyperparameters.length * 5 + 220;
    }
    if (data.tags && data.tags.length >= 6) {
      return 240;
    }

    return 200;
  };

  public isExternalFilterPresent = () => {
    return this.props.filtered;
  };

  public funEvaluate(filter: any) {
    // this.data is from the bind(node) where node is table row data
    // **ts forced creation of public data var to be able access node
    const operators: IOperator = {
      '<': (a: number, b: number) => a < b,
      '>': (a: number, b: number) => a > b
    };
    switch (filter.type) {
      case 'tag':
        return this.data.tags.includes(filter.key);
      case 'param':
        return this.data[filter.subtype].find((params: any) => params.key === filter.param)
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

  public doesExternalFilterPass = (node: any) => {
    return Object.values(this.props.filterState)
      .map(this.funEvaluate.bind(node))
      .every(val => val === true);
  };
}

// filterState and filtered should be provided by from IApplicationState -> customFilter
const mapStateToProps = ({ experimentRuns, dashboardConfig }: IApplicationState) => ({
  defaultColDefinitions,
  columnConfig: dashboardConfig.columnConfig,
  data: experimentRuns.data,
  loading: experimentRuns.loading,
  filterState: {},
  filtered: false
});

export default connect(mapStateToProps)(ExperimentRuns);
