import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { handleCustomErrorWithFallback } from 'shared/models/Error';
import ObservationsModelPage from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/ObservationsModelPage/ObservationsModelPage';
import vertaDocLinks from 'shared/utils/globalConstants/vertaDocLinks';
import { initialCommunication } from 'shared/utils/redux/communication';
import DeleteFAI from 'shared/view/elements/DeleteFAI/DeleteFAI';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import { PageCard, PageHeader } from 'shared/view/elements/PageComponents';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import * as ExperimentRunsStore from 'features/experimentRuns/store';
import * as ProjectsStore from 'features/projects/store';
import { IApplicationState } from 'setup/store/store';
import { hasAccessToAction } from 'shared/models/EntitiesActions';
import ClientSuggestion from 'shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';

import styles from './ModelRecord.module.css';
import VersionedInputsSection from './VersionedInputsSection/VersionedInputsSection';
import SummarySection from './SummarySection/SummarySection';
import HyperparametersSection from './HyperparametersSection/HyperparametersSection';
import MetricsSection from './MetricsSection/MetricsSection';
import Section from './shared/Section/Section';
import ArtifactsSection from './ArtifactsSection/ArtifactsSection';
import CodeVersionSection from './CodeVersionSection/CodeVersionSection';
import Artifacts from './shared/Artifacts/Artifacts';
import AttributesSection from './AttributesSection/AttributesSection';
import Reloading from 'shared/view/elements/Reloading/Reloading';
import WithCopyTextIcon from 'shared/view/elements/WithCopyTextIcon/WithCopyTextIcon';

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
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadModelRecord: ExperimentRunsStore.loadExperimentRun,
      deleteModelRecord: ExperimentRunsStore.deleteExperimentRun,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ILocalProps;

class ModelRecordView extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.loadModelRecord();
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
        () => { }
      );
    }

    if (prevProps.deleting.isRequesting && !this.props.deleting.isRequesting) {
      this.props.onDelete();
    }
  }

  public render() {
    const { data, loading, project, deleting } = this.props;

    if (loading.isRequesting || !project) {
      return (
        <div className={styles.preloader}>
          <Preloader variant="dots" />
        </div>
      );
    }
    if (loading.error || !data) {
      return <PageCommunicationError error={loading.error} />;
    }

    const versionedInputs = data.versionedInputs;
    return (
      <Reloading onReload={this.loadModelRecord}>
        <PageCard
          additionalClassname={cn({ [styles.deleting]: deleting.isRequesting })}
          dataTest="experiment-run"
        >
          <PageHeader
            title={
              <WithCopyTextIcon text={data.name}>
                <span data-test="experiment-run-name">{data.name}</span>
              </WithCopyTextIcon>
            }
            rightContent={
              hasAccessToAction('delete', data) && (
                <div className={styles.meta_id_container}>
                  <div className={styles.delete_button}>
                    <DeleteFAI
                      confirmText="Are you sure?"
                      faiDataTest="delete-experiment-run-button"
                      onDelete={this.deleteModelRecord}
                    />
                  </div>
                </div>
              )
            }
          />
          <SummarySection modelRecord={data} project={project} />
          {versionedInputs && (
            <VersionedInputsSection versionedInputs={versionedInputs} />
          )}
          <HyperparametersSection hyperparameters={data.hyperparameters} />
          <MetricsSection metrics={data.metrics} />
          <AttributesSection attributes={data.attributes} />
          <ArtifactsSection
            allowedActions={data.allowedActions}
            artifacts={data.artifacts}
            modelRecordId={data.id}
          />
          <CodeVersionSection
            id={data.id}
            codeVersion={data.codeVersion}
            codeVersionsFromBlob={data.codeVersionsFromBlob}
            versionedInputs={data.versionedInputs}
          />
          <Section iconType="observations" title="Observations">
            <ObservationsModelPage observations={data.observations} />
          </Section>
          <Section iconType="datasets" title="Datasets">
            {data.datasets.length > 0 ? (
              <Artifacts
                allowedActions={data.allowedActions}
                artifacts={data.datasets}
                modelRecordId={data.id}
              />
            ) : (
                <ClientSuggestion
                  fieldName={'dataset'}
                  clientMethod={'log_dataset_version()'}
                  link={vertaDocLinks.log_dataset}
                />
              )}
          </Section>
        </PageCard>
      </Reloading>
    );
  }

  @bind
  private loadModelRecord() {
    this.props.loadModelRecord(this.props.projectId, this.props.id);
  }

  @bind
  private deleteModelRecord() {
    this.props.deleteModelRecord(this.props.projectId, this.props.id);
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ModelRecordView);
