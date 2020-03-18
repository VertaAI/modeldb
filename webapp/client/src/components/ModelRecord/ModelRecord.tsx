import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { ShowCommentsButton } from 'features/comments';
import CodeVersion from 'components/CodeVersion/CodeVersion';
import ProjectEntityDescriptionManager from 'components/DescriptionManager/ProjectEntityDescriptionManager/ProjectEntityDescriptionManager';
import Artifacts from 'components/ModelRecordProps/Artifacts/Artifacts/Artifacts';
import Attributes from 'components/ModelRecordProps/Attributes/Attributes/Attributes';
import Datasets from 'components/ModelRecordProps/Datasets/Datasets';
import ObservationsModelPage from 'components/ModelRecordProps/Observations/Observations/ObservationsModelPage';
import ClientSuggestion from 'components/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import ProjectEntityTagsManager from 'components/TagsManager/ProjectEntityTagsManager/ProjectEntityTagsManager';
import { handleCustomErrorWithFallback } from 'core/shared/models/Error';
import { IHyperparameter } from 'core/shared/models/HyperParameters';
import { IMetric } from 'core/shared/models/Metrics';
import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';
import vertaDocLinks from 'core/shared/utils/globalConstants/vertaDocLinks';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import DeleteFAI from 'core/shared/view/elements/DeleteFAI/DeleteFAI';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import IdView from 'core/shared/view/elements/IdView/IdView';
import { PageCard } from 'core/shared/view/elements/PageComponents';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import * as ExperimentRunsStore from 'store/experimentRuns';
import * as ProjectsStore from 'store/projects';
import { IApplicationState } from 'store/store';

import styles from './ModelRecord.module.css';

interface ILocalProps {
  id: string;
  projectId: string;
  onShowNotFoundPage(error: any): void;
  onDelete(): void;
}

const mapStateToProps = (state: IApplicationState, localProps: ILocalProps) => {
  const modelRecord = ExperimentRunsStore.selectExperimentRun(
    state,
    localProps.id
  );
  return {
    data: modelRecord,
    loading:
      ExperimentRunsStore.selectCommunications(state).loadingExperimentRun[
        localProps.id
      ] || initialCommunication,
    project: ProjectsStore.selectProject(state, localProps.projectId),
    deleting:
      ExperimentRunsStore.selectCommunications(state).deletingExperimentRun[
        localProps.id
      ] || initialCommunication,
    deletingModelRecordArtifacts: ExperimentRunsStore.selectCommunications(
      state
    ).deletingExperimentRunArtifact,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadModelRecord: ExperimentRunsStore.loadExperimentRun,
      deleteModelRecord: ExperimentRunsStore.deleteExperimentRun,
      deleteModelRecordArtifact:
        ExperimentRunsStore.deleteExperimentRunArtifact,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ILocalProps;

class ModelRecordView extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.props.loadModelRecord(this.props.projectId, this.props.id);
  }

  public componentDidUpdate(prevProps: AllProps) {
    if (!prevProps.loading.error && this.props.loading.error) {
      handleCustomErrorWithFallback(
        this.props.loading.error,
        {
          accessDeniedToEntity: () =>
            this.props.onShowNotFoundPage(this.props.loading.error),
          entityNotFound: () =>
            this.props.onShowNotFoundPage(this.props.loading.error),
        },
        () => {}
      );
    }

    if (prevProps.deleting.isRequesting && !this.props.deleting.isRequesting) {
      this.props.onDelete();
    }
  }

  public render() {
    const {
      data,
      loading,
      project,
      deleting,
      deleteModelRecordArtifact,
      deletingModelRecordArtifacts,
    } = this.props;

    if (loading.isRequesting) {
      return (
        <div className={styles.preloader}>
          <Preloader variant="dots" />
        </div>
      );
    }
    if (loading.error || !data) {
      return <PageCommunicationError error={loading.error} />;
    }

    return (
      <PageCard
        additionalClassname={cn({ [styles.deleting]: deleting.isRequesting })}
        dataTest="experiment-run"
      >
        <div className={styles.record_summary}>
          <div className={styles.record_summary_main}>
            <div className={styles.header_records}>
              <div
                className={styles.record_name}
                data-test="experiment-run-name"
              >
                {data.name}
              </div>
            </div>
            <div className={styles.header_records}>
              <ProjectEntityDescriptionManager
                entityType={'experimentRun'}
                entityId={data.id}
                description={data.description}
              />
            </div>
            <div className={styles.header_records}>
              <div className={styles.tags_block}>
                <ProjectEntityTagsManager
                  id={data.id}
                  projectId={data.projectId}
                  tags={data.tags}
                  isDraggableTags={false}
                  entityType="experimentRun"
                />
              </div>
            </div>
          </div>
          <div className={styles.record_summary_meta}>
            <this.RenderModelIdMeta
              label="Run ID"
              valueTitle={data.id}
              id={data.id}
              copy={true}
              runId={true}
            >
              <IdView value={data.id} additionalClassName={styles.run_id} />
            </this.RenderModelIdMeta>
            <this.RenderModelIdMeta
              label="Experiment"
              valueTitle={data.shortExperiment.name}
              copy={true}
              id={data.experimentId}
            >
              {data.shortExperiment.name}
            </this.RenderModelIdMeta>
            <this.RenderModelIdMeta
              label="Project"
              valueTitle={project ? project.name : ''}
              copy={true}
              id={data.projectId}
            >
              {project ? project.name : ''}
            </this.RenderModelIdMeta>
            <this.RenderModelIdMeta
              label="Timestamp"
              valueTitle={getFormattedDateTime(data.dateCreated)}
            >
              {getFormattedDateTime(data.dateCreated)}
            </this.RenderModelIdMeta>
            <div
              className={styles.meta_id_container}
              style={{ marginTop: '5px' }}
            >
              <div className={styles.meta_label_container}>Comments</div>
              <ShowCommentsButton
                entityInfo={{ id: data.id, name: data.name }}
                buttonType="fai"
              />
            </div>
            <div className={styles.meta_id_container}>
              <label className={styles.meta_label_container}>Delete</label>
              <div className={styles.delete_button}>
                <DeleteFAI
                  confirmText="Are you sure?"
                  faiDataTest="delete-experiment-run-button"
                  onDelete={this.deleteModelRecord}
                />
              </div>
            </div>
          </div>
        </div>
        <this.Record
          label="Hyperparameters"
          additionalContainerClassName={styles.record_hyperparameters}
        >
          <div className={styles.parameter_pills_container}>
            {data.hyperparameters && data.hyperparameters.length > 0 && (
              <ScrollableContainer
                maxHeight={180}
                containerOffsetValue={12}
                children={
                  <>
                    {data.hyperparameters.map(
                      (hyperparameter: IHyperparameter, key: number) => {
                        const value =
                          typeof hyperparameter.value === 'number'
                            ? withScientificNotationOrRounded(
                                Number(hyperparameter.value)
                              )
                            : hyperparameter.value;
                        return (
                          <div key={key}>
                            <this.RenderModelMeta
                              label={hyperparameter.key}
                              valueTitle={String(value)}
                            >
                              {value}
                            </this.RenderModelMeta>
                          </div>
                        );
                      }
                    )}
                  </>
                }
              />
            )}
          </div>
          {data.hyperparameters && data.hyperparameters.length === 0 && (
            <ClientSuggestion
              fieldName={'hyperparameter'}
              clientMethod={'log_hyperparameters()'}
              link={vertaDocLinks.log_hyperparameters}
            />
          )}
        </this.Record>
        <this.Record
          label="Metrics"
          additionalContainerClassName={styles.record_metrics}
        >
          <div className={styles.parameter_pills_container}>
            {data.metrics && data.metrics.length > 0 && (
              <ScrollableContainer
                maxHeight={180}
                containerOffsetValue={12}
                children={
                  <>
                    {data.metrics.map((metric: IMetric, key: number) => {
                      const value =
                        typeof metric.value === 'number'
                          ? withScientificNotationOrRounded(
                              Number(metric.value)
                            )
                          : metric.value;
                      return (
                        <div key={key}>
                          <this.RenderModelMeta
                            label={metric.key}
                            valueTitle={String(value)}
                          >
                            {value}
                          </this.RenderModelMeta>
                        </div>
                      );
                    })}
                  </>
                }
              />
            )}
          </div>
          {data.metrics && data.metrics.length === 0 && (
            <ClientSuggestion
              fieldName={'metric'}
              clientMethod={'log_metric()'}
              link={vertaDocLinks.log_metric}
            />
          )}
        </this.Record>
        <this.Record
          label="Attributes"
          additionalContainerClassName={styles.record_attributes}
        >
          <Attributes
            attributes={data.attributes}
            pillSize="medium"
            docLink={vertaDocLinks.log_attribute}
          />
        </this.Record>
        <this.Record
          label="Artifacts"
          additionalContainerClassName={styles.record_artifacts}
        >
          <Artifacts
            entityType="experimentRun"
            pillSize="medium"
            entitiyId={data.id}
            artifacts={data.artifacts}
            docLink={vertaDocLinks.log_artifact}
            deletingInfo={{
              delete: deleteModelRecordArtifact,
              getDeleting: artifactKey =>
                deletingModelRecordArtifacts[artifactKey] ||
                initialCommunication,
              isCurrentUserCanDeleteArtifact: true,
            }}
          />
        </this.Record>
        <this.Record
          label="Code Version"
          additionalContainerClassName={styles.record_artifacts}
        >
          {data.codeVersion && (
            <CodeVersion
              entityId={data.id}
              entityType="experimentRun"
              codeVersion={data.codeVersion}
            />
          )}
          {!data.codeVersion && (
            <ClientSuggestion
              fieldName={'Code Version'}
              clientMethod={'log_code()'}
              link={vertaDocLinks.log_code}
            />
          )}
        </this.Record>
        <this.Record
          label="Observations"
          additionalContainerClassName={styles.record_observations}
        >
          <ObservationsModelPage
            observations={data.observations}
            docLink={vertaDocLinks.log_observations}
          />
        </this.Record>
        <this.Record
          label="Datasets"
          additionalContainerClassName={styles.record_datasets}
        >
          <Datasets
            modelId={data.id}
            datasets={data.datasets}
            docLink={vertaDocLinks.log_dataset}
            size="medium"
          />
        </this.Record>
      </PageCard>
    );
  }

  private RenderModelMeta(props: {
    label: string;
    children: React.ReactNode;
    valueTitle: string;
    copy?: boolean;
    id?: string;
    runId?: boolean;
  }) {
    const { id, label, valueTitle, children } = props;
    return (
      <div className={styles.meta_container}>
        <div className={styles.meta_label_container} title={label}>
          {label}
        </div>
        <div className={styles.value_container}>
          <div className={styles.meta_value} title={valueTitle}>
            {children}
          </div>
        </div>
      </div>
    );
  }

  private RenderModelIdMeta(props: {
    label: string;
    children: React.ReactNode;
    valueTitle: string;
    copy?: boolean;
    id?: string;
    runId?: boolean;
  }) {
    const { id, label, valueTitle, children, copy, runId } = props;
    return (
      <div className={styles.meta_id_container}>
        <div className={styles.meta_label_container} title={label}>
          {label}
        </div>
        <div
          className={styles[copy ? 'value_id_container' : 'value_container']}
        >
          <div className={styles.meta_value} title={valueTitle}>
            {children}
          </div>
          {copy ? (
            <div>
              (
              {id && !runId ? (
                <span title={id}>
                  <IdView value={id.slice(0, 7)} />,{' '}
                </span>
              ) : (
                ''
              )}
              <CopyButton value={id || String(children)} />)
            </div>
          ) : (
            ''
          )}
        </div>
      </div>
    );
  }

  private Record(props: {
    label: string;
    children?: React.ReactNode;
    additionalContainerClassName?: string;
    additionalHeaderClassName?: string;
    additionalValueClassName?: string;
  }) {
    const {
      label,
      children,
      additionalContainerClassName = '',
      additionalHeaderClassName = '',
      additionalValueClassName = '',
    } = props;
    return (
      <div className={cn(styles.record, additionalContainerClassName)}>
        <div className={cn(styles.record_label, additionalHeaderClassName)}>
          {label}
        </div>
        <div className={cn(styles.record_value, additionalValueClassName)}>
          <span>{children}</span>
        </div>
      </div>
    );
  }

  @bind
  private deleteModelRecord() {
    this.props.deleteModelRecord(this.props.projectId, this.props.id);
  }
}

export type IModelRecordActionProps = ReturnType<typeof mapDispatchToProps>;
export type IModelRecordProps = AllProps;
export { ModelRecordView };
export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ModelRecordView);
