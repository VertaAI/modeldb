import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { IHyperparameter } from '../../models/HyperParameters';
import { IMetric } from '../../models/Metrics';
import { IArtifact } from '../../models/Artifact';
import ModelRecord from '../../models/ModelRecord';
import { fetchModelRecord } from '../../store/model-record';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import ShowContentBasedOnUrl from '../ShowContentBasedOnUrl/ShowContentBasedOnUrl';
import styles from './ModelRecord.module.css';

export interface IUrlProps {
  modelId: string;
  projectId: string;
}

interface IPropsFromState {
  model_record?: ModelRecord | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;

class ModelRecordLayout extends React.Component<AllProps> {
  public render() {
    const { model_record, loading } = this.props;
    const notNullModel = model_record || new ModelRecord();

    return (
      <div className={styles.model_layout}>
        {this.renderTextRecord('Name', notNullModel.Name, styles.name)}
        {this.renderTextRecord(
          `ID${notNullModel.CodeVersion ? `, version` : ''}`,
          `${notNullModel.Id}${notNullModel.CodeVersion ? `, ${notNullModel.CodeVersion}` : ''}`
        )}
        {this.renderTextRecord('Project', notNullModel.ProjectId)}
        {this.renderTextRecord('Experiment', notNullModel.ExperimentId)}
        {this.renderTextRecord('Code version', notNullModel.CodeVersion)}
        {this.renderRecord(
          'Tags',
          notNullModel.Tags.map((value: string, key: number) => {
            return (
              <div className={styles.tag} key={key}>
                <span className={styles.tag_text}>{value}</span>
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'Hyperparameters',
          notNullModel.Hyperparameters.map((value: IHyperparameter, key: number) => {
            return (
              <div key={key}>
                {value.key}: {value.value}
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'Metrics',
          notNullModel.Metric.map((value: IMetric, key: number) => {
            return (
              <div key={key}>
                {value.key}: {value.value}
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'Artifacts',
          notNullModel.Artifacts.map((value: IArtifact, key: number) => {
            return (
              <div key={key}>
                {value.key}: <ShowContentBasedOnUrl path={value.path} />
              </div>
            );
          })
        )}
      </div>
    );
  }

  public componentDidMount() {
    this.props.dispatch(fetchModelRecord(this.props.match.params.modelId));
  }

  private renderRecord(header: string, content: JSX.Element[], additionalValueClassName: string = '') {
    return content && content.length > 0 ? (
      <div className={styles.record}>
        <div className={styles.record_header}>{header}</div>
        <div className={`${styles.record_value} ${additionalValueClassName}`}>{content}</div>
      </div>
    ) : (
      ''
    );
  }

  private renderTextRecord(header: string, value: string, additionalValueClassName: string = '') {
    return value ? this.renderRecord(header, [<span key={0}>{value}</span>], additionalValueClassName) : '';
  }

  private renderListRecord(header: string, content: JSX.Element[]) {
    return this.renderRecord(header, content, styles.list);
  }
}

const mapStateToProps = ({ model_record }: IApplicationState) => ({
  model_record: model_record.data,
  loading: model_record.loading
});

export default connect(mapStateToProps)(ModelRecordLayout);
