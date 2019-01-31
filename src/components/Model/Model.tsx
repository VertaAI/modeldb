import React from 'react';
import styles from './ModelRecord.module.css';

export default class ModelRecord extends React.PureComponent {
  public render() {
    return <div>Model Record</div>;
  }
}

// import * as React from 'react';
// import { connect } from 'react-redux';
// import { RouteComponentProps } from 'react-router';

// import { IHyperparameter } from 'models/HyperParameters';
// import { IModelMetric } from 'models/ModelMetric';
// import { IArtifact } from '../../models/Artifact';
// import { Model } from '../../models/Model';
// import { fetchModel } from '../../store/model';
// import { IApplicationState, IConnectedReduxProps } from '../../store/store';
// import ShowContentBasedOnUrl from '../ShowContentBasedOnUrl/ShowContentBasedOnUrl';
// import styles from './Model.module.css';

// export interface IUrlProps {
//   modelId: string;
//   projectId: string;
// }

// interface IPropsFromState {
//   data?: Model | null;
//   loading: boolean;
// }

// type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;

// class ModelLayout extends React.Component<AllProps> {
//   public render() {
//     const { data, loading } = this.props;
//     const notNullModel = data || new Model();

//     return (
//       <div className={styles.model_layout}>
//         {this.renderTextRecord('Name', notNullModel.Name, styles.name)}
//         {this.renderTextRecord('Description', notNullModel.Description)}
//         {this.renderTextRecord(
//           `ID${notNullModel.Version ? `, version` : ''}`,
//           `${notNullModel.Id}${notNullModel.Version ? `, ${notNullModel.Version}` : ''}`
//         )}
//         {this.renderTextRecord('Project', notNullModel.ProjectId)}
//         {this.renderTextRecord('Experiment', notNullModel.ExperimentId)}
//         {this.renderTextRecord('Date created', `${notNullModel.DateCreated ? notNullModel.DateCreated.toDateString() : ''}`)}
//         {this.renderTextRecord('Date updated', `${notNullModel.DateUpdated ? notNullModel.DateUpdated.toDateString() : ''}`)}
//         {this.renderTextRecord('Code version', notNullModel.CodeVersion)}
//         {this.renderRecord(
//           'Tags',
//           notNullModel.Tags.map((value: string, key: number) => {
//             return (
//               <div className={styles.tag} key={key}>
//                 <span className={styles.tag_text}>{value}</span>
//               </div>
//             );
//           })
//         )}
//         {this.renderListRecord(
//           'Hyperparameters',
//           notNullModel.Hyperparameters.map((value: IHyperparameter, key: number) => {
//             return (
//               <div key={key}>
//                 {value.key}: {value.value}
//               </div>
//             );
//           })
//         )}
//         {this.renderListRecord(
//           'Metrics',
//           notNullModel.ModelMetric.map((value: IModelMetric, key: number) => {
//             return (
//               <div key={key}>
//                 {value.key}: {value.value}
//               </div>
//             );
//           })
//         )}
//         {this.renderListRecord(
//           'DataSets',
//           notNullModel.DataSets.map((value: IArtifact, key: number) => {
//             return (
//               <div key={key}>
//                 {value.key}: <ShowContentBasedOnUrl path={value.path} />
//               </div>
//             );
//           })
//         )}
//         {this.renderListRecord(
//           'Artifacts',
//           notNullModel.Artifacts.map((value: IArtifact, key: number) => {
//             return (
//               <div key={key}>
//                 {value.key}: <ShowContentBasedOnUrl path={value.path} />
//               </div>
//             );
//           })
//         )}
//       </div>
//     );
//   }

//   public componentDidMount() {
//     this.props.dispatch(fetchModel(this.props.match.params.modelId));
//   }

//   private renderRecord(header: string, content: JSX.Element[], additionalValueClassName: string = '') {
//     return content && content.length > 0 ? (
//       <div className={styles.record}>
//         <div className={styles.record_header}>{header}</div>
//         <div className={`${styles.record_value} ${additionalValueClassName}`}>{content}</div>
//       </div>
//     ) : (
//       ''
//     );
//   }

//   private renderTextRecord(header: string, value: string, additionalValueClassName: string = '') {
//     return value ? this.renderRecord(header, [<span key={0}>{value}</span>], additionalValueClassName) : '';
//   }

//   private renderListRecord(header: string, content: JSX.Element[]) {
//     return this.renderRecord(header, content, styles.list);
//   }
// }

// const mapStateToProps = ({ model }: IApplicationState) => ({
//   data: model.data,
//   loading: model.loading
// });

// export default connect(mapStateToProps)(ModelLayout);
