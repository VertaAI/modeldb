import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import loader from 'components/images/loader.gif';
import tagStyles from 'components/shared/TagBlock/TagBlock.module.css';
import { IArtifact } from 'models/Artifact';
import { IHyperparameter } from 'models/HyperParameters';
import { IMetric } from 'models/Metrics';
import ModelRecord from 'models/ModelRecord';
import routes, { GetRouteParams } from 'routes';
import {
  fetchModelRecord,
  selectIsLoadingModelRecord,
  selectModelRecord,
} from 'store/model-record';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import styles from './ModelRecord.module.css';
import ShowContentBasedOnUrl from './ShowContentBasedOnUrl/ShowContentBasedOnUrl';

type IUrlProps = GetRouteParams<typeof routes.modelRecord>;

interface IPropsFromState {
  data?: ModelRecord | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;

class ModelRecordLayout extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(
      fetchModelRecord(this.props.match.params.modelRecordId)
    );
  }

  public render() {
    const { data, loading } = this.props;

    return loading ? (
      <img src={loader} className={styles.loader} />
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
            {this.renderParmaLink('Model ID:', data.id.slice(0, 4) + '..')}
            {this.renderParmaLink(
              'Project ID:',
              data.projectId.slice(0, 4) + '..'
            )}
            {this.renderParmaLink(
              'Experiment ID:',
              data.experimentId.slice(0, 4) + '..'
            )}
          </div>
        </div>
        {this.renderTextRecord('Code version', data.codeVersion)}
        {data.hyperparameters &&
          this.renderListRecord(
            'Hyperparameters',
            data.hyperparameters.map((value: IHyperparameter, key: number) => {
              return (
                <div key={key}>
                  {value.key}: {value.value}
                </div>
              );
            })
          )}
        {data.metrics &&
          this.renderListRecord(
            'Metrics',
            data.metrics.map((value: IMetric, key: number) => {
              return (
                <div key={key}>
                  {value.key}: {value.value}
                </div>
              );
            })
          )}
        {data.artifacts &&
          this.renderListRecord(
            'Artifacts',
            data.artifacts.map((value: IArtifact, key: number) => {
              return (
                <div key={key}>
                  {value.key}: <ShowContentBasedOnUrl path={value.path} />
                </div>
              );
            })
          )}
      </div>
    ) : (
      ''
    );
  }

  private renderRecord(
    header: string,
    content: JSX.Element[],
    additionalValueClassName: string = ''
  ) {
    return content && content.length > 0 ? (
      <div className={styles.record}>
        <div className={styles.record_header}>{header}</div>
        <div className={`${styles.record_value} ${additionalValueClassName}`}>
          {content}
        </div>
      </div>
    ) : (
      ''
    );
  }

  private renderParmaLink(label: string, value: string) {
    return (
      <div className={styles.experiment_link}>
        <span className={styles.parma_link_label}>{label}</span>{' '}
        <span className={styles.parma_link_value}>{value}</span>
      </div>
    );
  }

  private renderTextRecord(
    header: string,
    value: string,
    additionalValueClassName: string = ''
  ) {
    return value
      ? this.renderRecord(
          header,
          [<span key={0}>{value}</span>],
          additionalValueClassName
        )
      : '';
  }

  private renderListRecord(header: string, content: JSX.Element[]) {
    return this.renderRecord(header, content, styles.list);
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  data: selectModelRecord(state),
  loading: selectIsLoadingModelRecord(state),
});

export default connect(mapStateToProps)(ModelRecordLayout);
