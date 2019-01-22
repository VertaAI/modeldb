import { Cell, Column, Table } from 'fixed-data-table-2';
import 'fixed-data-table-2/dist/fixed-data-table.css';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { Dispatch } from 'redux';

import { Link } from 'react-router-dom';
import Project from '../../models/Project';
import { fetchProjectWithModels } from '../../store/project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { DataFrameCell } from './CellComponents/DataFrameCell';
import { IdCell } from './CellComponents/IdCell';
import { MetricsCell } from './CellComponents/MetricsCell';
import { MiscCell } from './CellComponents/MiscCell';
import { SpecificationsCell } from './CellComponents/SpecificationsCell';
import styles from './Models.module.css';
import './ModelsGrid.css';

export interface IModelsProps {
  projectId: string;
}

interface IPropsFromState {
  data?: Project | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IModelsProps> & IPropsFromState & IConnectedReduxProps;

class Models extends React.Component<AllProps> {
  public render() {
    const { data, loading } = this.props;
    const notNullData = data || new Project();
    return (
      <div className={styles.models_grid}>
        <div className={styles.project_header}>
          <div>
            <Link className={styles.path_copy} to={`/`}>
              <i className="fa fa-angle-left" />
            </Link>
            <span className={styles.project_name}>{notNullData.Name}</span>
          </div>
        </div>
        <div className={styles.filter} />
        <div className={styles.grid_layout}>
          <Table
            className={styles.table_wrapper}
            rowHeight={155}
            rowsCount={notNullData.Models.length}
            width={1024}
            height={1000}
            headerHeight={42}
          >
            <Column
              header={<Cell className={styles.table_header}>IDs</Cell>}
              cell={<IdCell className={styles.table_cell} models={notNullData.Models} />}
              width={184}
            />
            <Column
              header={<Cell className={styles.table_header}>DataFrame</Cell>}
              cell={<DataFrameCell className={styles.table_cell} models={notNullData.Models} />}
              width={184}
            />
            <Column
              header={<Cell className={styles.table_header}>Specifications</Cell>}
              cell={<SpecificationsCell className={styles.table_cell} models={notNullData.Models} />}
              width={203}
            />
            <Column
              header={<Cell className={styles.table_header}>Metrics</Cell>}
              cell={<MetricsCell className={styles.table_cell} models={notNullData.Models} />}
              width={148}
            />
            <Column
              header={<Cell className={styles.table_header}>Misc</Cell>}
              cell={<MiscCell className={styles.table_cell} models={notNullData.Models} />}
              width={290}
            />
          </Table>
        </div>
      </div>
    );
  }

  public componentDidMount() {
    this.props.dispatch(fetchProjectWithModels(this.props.match.params.projectId));
  }
}

const mapStateToProps = ({ project }: IApplicationState) => ({
  data: project.data,
  loading: project.loading
});

export default connect(mapStateToProps)(Models);
