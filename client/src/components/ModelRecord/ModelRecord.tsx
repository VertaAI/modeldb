import * as React from 'react';
import { connect, ReactReduxContextValue } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import {
  DeployButton,
  DeployManager,
  DeployServiceChart,
  DeployDataChart,
} from 'components/Deploy';
import loader from 'components/images/loader.gif';
import tagStyles from 'components/TagBlock/TagBlock.module.css';
import { IArtifact } from 'models/Artifact';
import { IHyperparameter } from 'models/HyperParameters';
import { IMetric } from 'models/Metrics';
import ModelRecord from 'models/ModelRecord';
import routes, { GetRouteParams } from 'routes';
import {
  checkDeployStatusUntilDeployed,
  fetchDataStatisticsActionTypes,
  getDataStatistics,
  getServiceStatistics,
  selectDataStatistics,
  selectDeployStatusInfo,
  selectIsLoadingDataStatistics,
  selectIsLoadingServiceStatistics,
  selectServiceStatistics,
} from 'store/deploy';
import { fetchModelRecord } from 'store/model-record';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
  IUnknownStatusInfo,
} from 'models/Deploy';
import styles from './ModelRecord.module.css';
import ShowContentBasedOnUrl from './ShowContentBasedOnUrl/ShowContentBasedOnUrl';

type IUrlProps = GetRouteParams<typeof routes.modelRecord>;

interface IPropsFromState {
  data: ModelRecord | null;
  loadingModelRecord: boolean;
  deployState: IDeployStatusInfo | null;
  serviceStatistics: IServiceStatistics | null;
  dataStatistics: IDataStatistics | null;
  loadingServiceStatistics: boolean;
  loadingDataStatistics: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;

class ModelRecordLayout extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.props.dispatch(
      fetchModelRecord(this.props.match.params.modelRecordId)
    );
    if (this.props.data) {
      this.props.dispatch(checkDeployStatusUntilDeployed(this.props.data.id));
      if (
        this.props.deployState &&
        this.props.deployState.status === 'deployed'
      ) {
        this.props.dispatch(getServiceStatistics(this.props.data.id));
        this.props.dispatch(getDataStatistics(this.props.data.id));
      }
    }
  }

  public componentDidUpdate(prevProps: AllProps) {
    if (this.props.data && prevProps.data !== this.props.data) {
      this.props.dispatch(checkDeployStatusUntilDeployed(this.props.data.id));
    }

    if (
      this.props.data &&
      ((prevProps.deployState && prevProps.deployState.status !== 'deployed') ||
        !prevProps.deployState) &&
      this.props.deployState &&
      this.props.deployState.status === 'deployed'
    ) {
      this.props.dispatch(getServiceStatistics(this.props.data.id));
      this.props.dispatch(getDataStatistics(this.props.data.id));
    }
  }

  public render() {
    const { data, loadingModelRecord: loading, deployState } = this.props;
    const alreadyDeployed = deployState && deployState.status === 'deployed';

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : data ? (
      <div className={styles.model_layout}>
        <div className={styles.record_summary}>
          <div className={styles.record_summary_main}>
            <div className={styles.record_label}>Name</div>
            <div className={styles.record_name}>{data.name}</div>
            <br />
            {data.tags && <div className={styles.record_label}>Tags</div>}
            <div>
              {data.tags &&
                data.tags.map((value: string, key: number) => {
                  return (
                    <div className={styles.tags} key={key}>
                      <span className={tagStyles.staticTag}>{value}</span>
                    </div>
                  );
                })}
            </div>
          </div>
          <div className={styles.record_summary_meta}>
            <this.ParmaLink label="Model ID:" value={data.id} />
            <this.ParmaLink label="Project ID:" value={data.projectId} />
            <this.ParmaLink label="Experiment ID:" value={data.experimentId} />
          </div>
        </div>
        {data.codeVersion! && (
          <this.Record header="Code version">{data.codeVersion}</this.Record>
        )}
        {data.hyperparameters && (
          <this.Record header="Hyperparameters">
            {data.hyperparameters.map((value: IHyperparameter, key: number) => {
              return (
                <div key={key}>
                  {value.key}: {value.value}
                </div>
              );
            })}
          </this.Record>
        )}
        {data.metrics && (
          <this.Record header="Metrics">
            {data.metrics.map((value: IMetric, key: number) => {
              return (
                <div key={key}>
                  {value.key}: {value.value}
                </div>
              );
            })}
          </this.Record>
        )}
        {data.artifacts && (
          <this.Record header="Artifacts">
            {data.artifacts.map((value: IArtifact, key: number) => {
              return (
                <div key={key}>
                  {value.key}: <ShowContentBasedOnUrl path={value.path} />
                </div>
              );
            })}
          </this.Record>
        )}
        {data.datasets && (
          <this.Record header="Datasets">
            {data.datasets.map((value: IArtifact, key: number) => {
              return (
                <div key={key}>
                  {value.key}: <ShowContentBasedOnUrl path={value.path} />
                </div>
              );
            })}
          </this.Record>
        )}
        <this.Record header="Deploy info">
          <>
            <DeployButton modelId={data.id} />
            <DeployManager />
          </>
        </this.Record>
        <this.Record
          header="Monitoring information"
          additionalHeaderClassName={styles.record_header_divider}
          additionalContainerClassName={styles.record_divider}
        />
        {(!deployState || deployState.status !== 'deployed') && (
          <div className={styles.record_header}>No monitoring information</div>
        )}
        {alreadyDeployed &&
          (!this.props.serviceStatistics ||
            !this.props.serviceStatistics.time) && (
            <div className={styles.record_header}>
              No monitoring information in the time window considered
            </div>
          )}
        {alreadyDeployed &&
          this.props.serviceStatistics &&
          this.props.serviceStatistics.time && (
            <div className={styles.chart}>
              {this.props.loadingServiceStatistics ? (
                <img src={loader} className={styles.loader} />
              ) : this.props.serviceStatistics ? (
                //JSON.stringify(this.props.serviceStatistics)
                <React.Fragment>
                  <this.Record header="Service behavior">
                    <DeployServiceChart
                      height={400}
                      width={600}
                      marginBottom={80}
                      marginLeft={60}
                      marginTop={40}
                      marginRight={60}
                      modelId={data.id}
                      //metrics={this.props.serviceStatistics}
                    />
                  </this.Record>
                </React.Fragment>
              ) : (
                ''
              )}
            </div>
          )}
        {alreadyDeployed &&
          this.props.dataStatistics &&
          this.props.dataStatistics.size > 0 && (
            <div className={styles.chart}>
              {this.props.loadingServiceStatistics ? (
                <img src={loader} className={styles.loader} />
              ) : this.props.dataStatistics ? (
                //JSON.stringify(this.props.serviceStatistics)
                <React.Fragment>
                  <this.Record header="Data behavior">
                    <DeployDataChart
                      height={400}
                      width={600}
                      marginBottom={20}
                      marginLeft={60}
                      marginTop={40}
                      marginRight={60}
                      modelId={data.id}
                      //statistics={this.props.dataStatistics}
                    />
                  </this.Record>
                </React.Fragment>
              ) : (
                ''
              )}
            </div>
          )}
      </div>
    ) : (
      ''
    );
  }

  // tslint:disable-next-line:function-name
  private Record(props: {
    header: string;
    children?: React.ReactNode;
    additionalValueClassName?: string;
    additionalContainerClassName?: string;
    additionalHeaderClassName?: string;
  }) {
    const {
      header,
      children,
      additionalValueClassName,
      additionalContainerClassName,
      additionalHeaderClassName,
    } = props;
    return (
      <div className={`${styles.record} ${additionalContainerClassName}`}>
        <div className={`${styles.record_header} ${additionalHeaderClassName}`}>
          {header}
        </div>
        <div className={`${styles.record_value} ${additionalValueClassName}`}>
          {children}
        </div>
      </div>
    );
  }

  // tslint:disable-next-line:function-name
  private ParmaLink(props: { label: string; value: string }) {
    const { label, value } = props;
    return (
      <div className={styles.experiment_link}>
        <span className={styles.parma_link_label}>{label}</span>{' '}
        <span className={styles.parma_link_value}>{value.slice(0, 4)}..</span>
      </div>
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  const { modelRecord } = state;
  return {
    data: modelRecord.data,
    dataStatistics: selectDataStatistics(state),
    deployState: modelRecord.data
      ? selectDeployStatusInfo(state, modelRecord.data.id)
      : null,
    loadingDataStatistics: selectIsLoadingDataStatistics(state),
    loadingModelRecord: modelRecord.loading,
    loadingServiceStatistics: selectIsLoadingServiceStatistics(state),
    serviceStatistics: selectServiceStatistics(state),
  };
};

export default connect(mapStateToProps)(ModelRecordLayout);
