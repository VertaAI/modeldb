import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import { DeployButton, DeployManager } from 'components/Deploy';
import Preloader from 'components/shared/Preloader/Preloader';
import tagStyles from 'components/shared/TagBlock/TagBlock.module.css';
import { IArtifact } from 'models/Artifact';
import { IHyperparameter } from 'models/HyperParameters';
import { IMetric } from 'models/Metrics';
import ModelRecord from 'models/ModelRecord';
import routes, { GetRouteParams } from 'routes';
import {
  checkDeployStatusUntilDeployed,
  getDataStatistics,
  getServiceStatistics,
  selectDataStatistics,
  selectDeployStatusInfo,
  selectIsLoadingDataStatistics,
  selectIsLoadingServiceStatistics,
  selectServiceStatistics,
} from 'store/deploy';
import {
  fetchModelRecord,
  selectIsLoadingModelRecord,
  selectModelRecord,
} from 'store/model-record';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
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
      <div className={styles.loader}>
        <Preloader variant="dots" />
      </div>
    ) : data ? (
      <div className={styles.model_layout}>
        <div className={styles.record_summary}>
          <div className={styles.record_summary_main}>
            <div className={styles.record_label}>Name</div>
            <div className={styles.record_name}>{data.name}</div>
            <br />
            <div className={styles.record_label}>Tags</div>
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
        <this.Record header="Code version">{data.codeVersion}</this.Record>
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
          <this.Record header="No monitoring information" />
        )}
        {alreadyDeployed && (
          <this.Record header="Latency and service metrics">
            {this.props.loadingServiceStatistics ? (
              <div className={styles.loader}>
                <Preloader variant="dots" />
              </div>
            ) : this.props.serviceStatistics ? (
              JSON.stringify(this.props.serviceStatistics)
            ) : (
              ''
            )}
          </this.Record>
        )}
        {alreadyDeployed && (
          <this.Record header="Data metrics">
            {this.props.loadingDataStatistics ? (
              <div className={styles.loader}>
                <Preloader variant="dots" />
              </div>
            ) : this.props.dataStatistics ? (
              JSON.stringify(this.props.dataStatistics)
            ) : (
              ''
            )}
          </this.Record>
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
  const modelRecord = selectModelRecord(state);
  return {
    data: modelRecord,
    dataStatistics: selectDataStatistics(state),
    deployState: modelRecord
      ? selectDeployStatusInfo(state, modelRecord.id)
      : null,
    loadingDataStatistics: selectIsLoadingDataStatistics(state),
    loadingModelRecord: selectIsLoadingModelRecord(state),
    loadingServiceStatistics: selectIsLoadingServiceStatistics(state),
    serviceStatistics: selectServiceStatistics(state),
  };
};

export default connect(mapStateToProps)(ModelRecordLayout);
