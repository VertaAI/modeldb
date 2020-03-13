import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import TagsField from 'core/shared/view/formComponents/formikFields/TagsFieldWithTopLabel/TagsFieldWithTopLabel';
import TextInputFieldWithTopLabel from 'core/shared/view/formComponents/formikFields/TextInputFieldWithTopLabel/TextInputFieldWithTopLabel';
import PresetFormik from 'core/shared/view/formComponents/presetComponents/PresetFormik/PresetFormik';
import { validateDescription } from 'core/shared/models/Description';
import { handleCustomErrorWithFallback } from 'core/shared/models/Error';
import { validateNotEmpty } from 'core/shared/utils/validators';
import Button from 'core/shared/view/elements/Button/Button';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import * as Experiment from 'models/Experiment';
import routes, { GetRouteParams } from 'routes';
import * as ExperimentsStore from 'store/experiments';
import { IApplicationState } from 'store/store';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import styles from './ExperimentCreationPage.module.css';

const mapStateToProps = (state: IApplicationState) => {
  return {
    creatingExperiment: ExperimentsStore.selectCommunications(state)
      .creatingExperiment,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      createExperiment: ExperimentsStore.createExperiment,
      resetCreatingExperiment: ExperimentsStore.createExperiment.reset,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  RouteComponentProps<GetRouteParams<typeof routes.experimentCreation>>;

const initialSettings: Experiment.IExperimentCreationSettings = {
  name: '',
  description: '',
  tags: [],
};

class ExperimentCreationPage extends React.PureComponent<AllProps> {
  public UNSAFE_componentWillMount() {
    this.props.resetCreatingExperiment(undefined);
  }

  public render() {
    const { creatingExperiment } = this.props;

    return (
      <ProjectsPagesLayout>
        <PageCard>
          <PageHeader title="Create a new experiment" />
          <PresetFormik<Experiment.IExperimentCreationSettings>
            initialValues={initialSettings}
            onSubmit={this.createExperiment}
          >
            {({ isValid }) => (
              <>
                <div className={styles.settings}>
                  <div className={styles.section}>
                    <TextInputFieldWithTopLabel
                      name="name"
                      dataTest="name"
                      label="Experiment name"
                      size="medium"
                      validate={validateNotEmpty('Experiment name')}
                      isRequired={true}
                    />
                    <TextInputFieldWithTopLabel
                      name="description"
                      dataTest="description"
                      label="Description"
                      size="medium"
                      validate={validateDescription}
                    />
                  </div>
                  <div className={styles.section}>
                    <TagsField name="tags" />
                  </div>
                </div>
                <div className={styles.submit}>
                  <Button
                    isLoading={creatingExperiment.isRequesting}
                    disabled={!isValid}
                    dataTest="create"
                    type="submit"
                  >
                    Create experiment
                  </Button>
                </div>
                {creatingExperiment.error && (
                  <div className={styles.error}>
                    {(() => {
                      return handleCustomErrorWithFallback(
                        creatingExperiment.error,
                        {
                          entityAlreadyExists: () => (
                            <InlineErrorView
                              error={
                                'Experiment with such name already exists!'
                              }
                            />
                          ),
                        },
                        error => <InlineCommunicationError error={error} />
                      );
                    })()}
                  </div>
                )}
              </>
            )}
          </PresetFormik>
        </PageCard>
      </ProjectsPagesLayout>
    );
  }

  @bind
  private createExperiment(settings: Experiment.IExperimentCreationSettings) {
    this.props.createExperiment({
      projectId: this.props.match.params.projectId,
      settings,
    });
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ExperimentCreationPage);
