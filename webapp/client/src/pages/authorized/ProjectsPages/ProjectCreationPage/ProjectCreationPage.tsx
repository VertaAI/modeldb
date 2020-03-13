import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
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
import Select from 'core/shared/view/elements/Selects/Select/Select';
import { IProjectCreationSettings } from 'models/Project';
import {
  AuthorizedLayout,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'routes';
import {
  createProject,
  selectCommunications,
  resetCreateProjectCommunication,
} from 'store/projectCreation';
import { IApplicationState } from 'store/store';

import styles from './ProjectCreationPage.module.css';

const mapStateToProps = (state: IApplicationState) => {
  return {
    creatingProject: selectCommunications(state).creatingProject,
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

class ProjectCreationPage extends React.PureComponent<AllProps> {
  private breadcrumbsBuilder = BreadcrumbsBuilder()
    .then({ routes: [routes.projects], getName: () => 'Projects' })
    .then({
      routes: [routes.projectCreation],
      getName: () => 'Project creation',
    });

  private initialSettings: IProjectCreationSettings = {
    name: '',
    visibility: 'private',
    description: '',
    tags: [],
  };

  public UNSAFE_componentWillMount() {
    this.props.resetCreateProjectCommunication();
  }

  public render() {
    const { creatingProject } = this.props;

    return (
      <AuthorizedLayout breadcrumbsBuilder={this.breadcrumbsBuilder}>
        <PageCard>
          <PageHeader title="Create a new project" />
          <PresetFormik<IProjectCreationSettings>
            initialValues={this.initialSettings}
            onSubmit={this.createProject}
          >
            {({ isValid, values }) => (
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
      </AuthorizedLayout>
    );
  }

  @bind
  private createProject(settings: IProjectCreationSettings) {
    this.props.createProject(settings);
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ProjectCreationPage);
