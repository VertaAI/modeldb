import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import {
  DeployButton,
  DeployDataChart,
  DeployManager,
  DeployServiceChart,
} from 'components/Deploy';
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
            {data.tags && data.tags.length > 0 && (
              <div>
                <div className={styles.record_label}>Tags</div>
                <div>
                  {data.tags.map((value: string, key: number) => {
                    return (
                      <div className={styles.tags} key={key}>
                        <span className={tagStyles.staticTag}>{value}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
          <div className={styles.record_summary_meta}>
            <this.RenderModelMeta label="Id" value={data.id} />
            <this.RenderModelMeta
              label="Experiment"
              value={data.experimentId}
            />
            <this.RenderModelMeta label="Project" value={data.projectId} />
          </div>
        </div>
        {data.codeVersion && (
          <this.Record header="Code version">{data.codeVersion}</this.Record>
        )}
        {data.hyperparameters && data.hyperparameters.length > 0 && (
          <this.Record header="Hyperparameters">
            {data.hyperparameters.map((val: IHyperparameter, key: number) => {
              return (
                <div key={key}>
                  <this.RenderModelMeta label={val.key} value={val.value} />
                </div>
              );
            })}
          </this.Record>
        )}
        {data.metrics && data.metrics.length > 0 && (
          <this.Record header="Metrics">
            {data.metrics.map((val: IMetric, key: number) => {
              return (
                <div key={key}>
                  <this.RenderModelMeta label={val.key} value={val.value} />
                </div>
              );
            })}
          </this.Record>
        )}
        {data.artifacts && data.artifacts.length > 0 && (
          <this.Record header="Artifacts">
            {data.artifacts.map((value: IArtifact, key: number) => {
              return (
                <div key={key}>
                  <this.RenderModelMeta
                    label={value.key}
                    children={<ShowContentBasedOnUrl path={value.path} />}
                  />
                </div>
              );
            })}
          </this.Record>
        )}
        {data.datasets && data.datasets.length > 0 && (
          <this.Record header="Datasets">
            {data.datasets.map((value: IArtifact, key: number) => {
              return (
                <div key={key}>
                  <this.RenderModelMeta
                    label={value.key}
                    children={<ShowContentBasedOnUrl path={value.path} />}
                  />
                </div>
              );
            })}
          </this.Record>
        )}
        <this.Record header="Deploy info">
          <DeployButton modelId={data.id} />
          <DeployManager />
        </this.Record>
        <this.Record
          header="Monitoring information"
          additionalHeaderClassName={styles.record_header_divider}
          additionalContainerClassName={styles.record_divider}
        />
        {(!deployState || deployState.status !== 'deployed') && (
          <div className={styles.notDeployedMsg}>
            <i className="fa fa-exclamation-triangle" />
            <span>No monitoring information</span>
          </div>
        )}
        {alreadyDeployed &&
          (!this.props.serviceStatistics ||
            !this.props.serviceStatistics.time) && (
            <div className={styles.notDeployedMsg}>
              <i className="fa fa-exclamation-triangle" />
              <span>
                No monitoring information in the time window considered
              </span>
            </div>
          )}
        {alreadyDeployed &&
          this.props.serviceStatistics &&
          this.props.serviceStatistics.time && (
            <div className={styles.chart}>
              {this.props.loadingServiceStatistics ? (
                <Preloader variant="dots" />
              ) : this.props.serviceStatistics ? (
                <>
                  <this.Record header="Service behavior">
                    <DeployServiceChart
                      height={400}
                      width={600}
                      marginBottom={80}
                      marginLeft={80}
                      marginTop={40}
                      marginRight={80}
                      modelId={data.id}
                    />
                  </this.Record>
                </>
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
                <Preloader variant="dots" />
              ) : this.props.dataStatistics ? (
                <>
                  <this.Record header="Data behavior">
                    <DeployDataChart
                      height={400}
                      width={600}
                      marginBottom={40}
                      marginLeft={60}
                      marginTop={60}
                      marginRight={60}
                      modelId={data.id}
                    />
                  </this.Record>
                </>
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
  private RenderModelMeta(props: {
    label: string;
    value?: string | number;
    children?: React.ReactNode;
  }) {
    const { label, value, children } = props;
    return (
      <div className={styles.meta_block}>
        <div className={styles.meta_label}>{`${label} :`}</div>
        {value ? (
          <div className={styles.meta_value}>{value}</div>
        ) : (
          <div>{children}</div>
        )}
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
