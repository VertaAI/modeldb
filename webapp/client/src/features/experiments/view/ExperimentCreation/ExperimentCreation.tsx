import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { validateDescription } from 'shared/models/Description';
import { handleCustomErrorWithFallback } from 'shared/models/Error';
import { validateNotEmpty } from 'shared/utils/validators';
import Button from 'shared/view/elements/Button/Button';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import InlineErrorView from 'shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import { PageCard, PageHeader } from 'shared/view/elements/PageComponents';
import TagsField from 'shared/view/formComponents/formikFields/TagsFieldWithTopLabel/TagsFieldWithTopLabel';
import TextInputFieldWithTopLabel from 'shared/view/formComponents/formikFields/TextInputFieldWithTopLabel/TextInputFieldWithTopLabel';
import PresetFormik from 'shared/view/formComponents/presetComponents/PresetFormik/PresetFormik';
import * as ExperimentsStore from 'features/experiments/store';
import * as Experiment from 'shared/models/Experiment';
import { IApplicationState } from 'store/store';

import styles from './ExperimentCreation.module.css';

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

interface ILocalProps {
  projectId: string;
}

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ILocalProps;

const initialSettings: Experiment.IExperimentCreationSettings = {
  name: '',
  description: '',
  tags: [],
};

class ExperimentCreation extends React.PureComponent<AllProps> {
  public UNSAFE_componentWillMount() {
    this.props.resetCreatingExperiment(undefined);
  }

  public render() {
    const { creatingExperiment } = this.props;

    return (
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
                            error={'Experiment with such name already exists!'}
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
    );
  }

  @bind
  private createExperiment(settings: Experiment.IExperimentCreationSettings) {
    this.props.createExperiment({
      projectId: this.props.projectId,
      settings,
    });
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ExperimentCreation);
