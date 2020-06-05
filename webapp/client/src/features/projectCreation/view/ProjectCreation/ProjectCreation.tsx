import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { validateDescription } from 'core/shared/models/Description';
import { handleCustomErrorWithFallback } from 'core/shared/models/Error';
import { validateNotEmpty } from 'core/shared/utils/validators';
import Button from 'core/shared/view/elements/Button/Button';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import TagsField from 'core/shared/view/formComponents/formikFields/TagsFieldWithTopLabel/TagsFieldWithTopLabel';
import TextInputFieldWithTopLabel from 'core/shared/view/formComponents/formikFields/TextInputFieldWithTopLabel/TextInputFieldWithTopLabel';
import PresetFormik from 'core/shared/view/formComponents/presetComponents/PresetFormik/PresetFormik';
import {
  createProject,
  selectCommunications,
  resetCreateProjectCommunication,
} from 'features/projectCreation';
import { IProjectCreationSettings } from 'models/Project';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspace } from 'features/workspaces/store';

import styles from './ProjectCreation.module.css';

const mapStateToProps = (state: IApplicationState) => {
  return {
    creatingProject: selectCommunications(state).creatingProject,
    currentWorkspace: selectCurrentWorkspace(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      createProject,
      resetCreateProjectCommunication,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

interface IProjectCreationForm
  extends Omit<IProjectCreationSettings, 'visibility'> {
  isOrgPublic?: boolean;
}

class ProjectCreation extends React.PureComponent<AllProps> {
  private initialSettings: IProjectCreationForm = {
    name: '',
    description: '',
    tags: [],
  };

  public UNSAFE_componentWillMount() {
    this.props.resetCreateProjectCommunication();
  }

  public render() {
    const { creatingProject, currentWorkspace } = this.props;

    return (
      <PageCard>
        <PageHeader title="Create a new project" />
        <PresetFormik<IProjectCreationForm>
          initialValues={this.initialSettings}
          onSubmit={this.createProject}
        >
          {({ values, setFieldValue, isValid }) => (
            <>
              <div className={styles.settings}>
                <div className={styles.section}>
                  <TextInputFieldWithTopLabel
                    name="name"
                    validate={validateNotEmpty('project name')}
                    label="Project name"
                    size="medium"
                    dataTest="name"
                    isRequired={true}
                  />
                  <TextInputFieldWithTopLabel
                    name="description"
                    validate={validateDescription}
                    label="Description"
                    size="medium"
                    dataTest="description"
                  />
                </div>
                <div className={styles.section}>
                  <TagsField name="tags" />
                </div>
              </div>
              <div className={styles.submit}>
                <Button
                  isLoading={creatingProject.isRequesting}
                  disabled={!isValid}
                  dataTest="create"
                  type="submit"
                >
                  Create project
                </Button>
              </div>
              {creatingProject.error &&
                handleCustomErrorWithFallback(
                  creatingProject.error,
                  {
                    projectAlreadyExists: () => (
                      // entityAlreadyExist
                      <InlineErrorView
                        error={'Project with such name already exists!'}
                      />
                    ),
                  },
                  error => <InlineCommunicationError error={error} />
                )}
            </>
          )}
        </PresetFormik>
      </PageCard>
    );
  }

  @bind
  private createProject(values: IProjectCreationForm) {
    const { name, description, tags } = values;

    this.props.createProject({
      name,
      description,
      tags,
      visibility: 'private',
    });
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ProjectCreation);
