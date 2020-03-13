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
import * as Dataset from 'models/Dataset';
import {
  AuthorizedLayout,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'routes';
import * as DatasetsStore from 'store/datasets';
import { IApplicationState } from 'store/store';

import styles from './DatasetCreationPage.module.css';

const mapStateToProps = (state: IApplicationState) => {
  return {
    creatingDataset: DatasetsStore.selectCommunications(state).creatingDataset,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      createDataset: DatasetsStore.createDataset,
      resetCreatingDataset: DatasetsStore.createDataset.reset,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

class DatasetCreationPage extends React.PureComponent<AllProps> {
  private breadcrumbsBuilder = BreadcrumbsBuilder()
    .then({ routes: [routes.datasets], getName: () => 'Datasets' })
    .then({
      routes: [routes.datasetCreation],
      getName: () => 'Dataset creation',
    });

  private initialSettings: Dataset.IDatasetCreationSettings = {
    description: '',
    name: '',
    tags: [],
    type: 'path',
    visibility: 'private',
  };

  public UNSAFE_componentWillMount() {
    this.props.resetCreatingDataset(undefined);
  }

  public render() {
    const { creatingDataset } = this.props;

    return (
      <AuthorizedLayout breadcrumbsBuilder={this.breadcrumbsBuilder}>
        <PageCard>
          <PageHeader title="Create a new dataset" />
          <PresetFormik<Dataset.IDatasetCreationSettings>
            initialValues={this.initialSettings}
            onSubmit={this.createDataset}
          >
            {({ values, isValid }) => (
              <>
                <div className={styles.settings}>
                  <div className={styles.section}>
                    <TextInputFieldWithTopLabel
                      name="name"
                      dataTest="name"
                      label="Dataset name"
                      size="medium"
                      isRequired={true}
                      validate={validateNotEmpty('Dataset name')}
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
                    isLoading={creatingDataset.isRequesting}
                    disabled={!isValid}
                    dataTest="create"
                    type="submit"
                  >
                    Create dataset
                  </Button>
                </div>
                {creatingDataset.error && (
                  <div className={styles.error}>
                    {(() => {
                      return handleCustomErrorWithFallback(
                        creatingDataset.error,
                        {
                          entityAlreadyExists: () => (
                            <InlineErrorView
                              error={'Dataset with such name already exists!'}
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
      </AuthorizedLayout>
    );
  }

  @bind
  private createDataset(settings: Dataset.IDatasetCreationSettings) {
    this.props.createDataset({ settings });
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DatasetCreationPage);
