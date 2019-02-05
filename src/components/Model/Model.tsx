import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';

import { IHyperparameter } from 'models/HyperParameters';
import { IModelMetric } from 'models/ModelMetric';
import { IArtifact } from '../../models/Artifact';
import { Model } from '../../models/Model';
import { fetchModel } from '../../store/model';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import loader from '../images/loader.gif';
import ShowContentBasedOnUrl from '../ShowContentBasedOnUrl/ShowContentBasedOnUrl';
import styles from './Model.module.css';

export interface IUrlProps {
  modelId: string;
  projectId: string;
}

interface IPropsFromState {
  data?: Model | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;

class ModelLayout extends React.Component<AllProps> {
  public render() {
    const { data, loading } = this.props;

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : data ? (
      <div className={styles.model_layout}>
        {this.renderTextRecord('Name', data.Name, styles.name)}
        {this.renderTextRecord('Description', data.Description)}
        {this.renderTextRecord(`ID${data.Version ? `, version` : ''}`, `${data.Id}${data.Version ? `, ${data.Version}` : ''}`)}
        {this.renderTextRecord('Project', data.ProjectId)}
        {this.renderTextRecord('Experiment', data.ExperimentId)}
        {this.renderTextRecord('Date created', `${data.DateCreated ? data.DateCreated.toDateString() : ''}`)}
        {this.renderTextRecord('Date updated', `${data.DateUpdated ? data.DateUpdated.toDateString() : ''}`)}
        {this.renderTextRecord('Code version', data.CodeVersion)}
        {this.renderRecord(
          'Tags',
          data.Tags.map((value: string, key: number) => {
            return (
              <div className={styles.tag} key={key}>
                <span className={styles.tag_text}>{value}</span>
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'Hyperparameters',
          data.Hyperparameters.map((value: IHyperparameter, key: number) => {
            return (
              <div key={key}>
                {value.key}: {value.value}
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'Metrics',
          data.ModelMetric.map((value: IModelMetric, key: number) => {
            return (
              <div key={key}>
                {value.key}: {value.value}
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'DataSets',
          data.DataSets.map((value: IArtifact, key: number) => {
            return (
              <div key={key}>
                {value.key}: <ShowContentBasedOnUrl path={value.path} />
              </div>
            );
          })
        )}
        {this.renderListRecord(
          'Artifacts',
          data.Artifacts.map((value: IArtifact, key: number) => {
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

  public componentDidMount() {
    this.props.dispatch(fetchModel(this.props.match.params.modelId));
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

const mapStateToProps = ({ model }: IApplicationState) => ({
  data: model.data,
  loading: model.loading
});

export default connect(mapStateToProps)(ModelLayout);
