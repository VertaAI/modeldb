import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { validateDescription } from 'shared/models/Description';
import { handleCustomErrorWithFallback } from 'shared/models/Error';
import { validateNotEmpty } from 'shared/utils/validators';
import Button from 'shared/view/elements/Button/Button';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import InlineErrorView from 'shared/view/elements/Errors/InlineErrorView/InlineErrorView';
import FieldWithTopLabel from 'shared/view/elements/FieldWithTopLabel/FieldWithTopLabel';
import { PageCard, PageHeader } from 'shared/view/elements/PageComponents';
import SelectField from 'shared/view/formComponents/formikFields/SelectField/SelectField';
import TagsField from 'shared/view/formComponents/formikFields/TagsFieldWithTopLabel/TagsFieldWithTopLabel';
import TextInputFieldWithTopLabel from 'shared/view/formComponents/formikFields/TextInputFieldWithTopLabel/TextInputFieldWithTopLabel';
import PresetFormik from 'shared/view/formComponents/presetComponents/PresetFormik/PresetFormik';
import * as DatasetsStore from 'features/datasets/store';
import * as Dataset from 'shared/models/Dataset';
import { IApplicationState } from 'setup/store/store';
import {
  selectWorkspaces,
  selectCurrentWorkspaceName,
} from 'features/workspaces/store';

import styles from './DatasetCreation.module.css';

const mapStateToProps = (state: IApplicationState) => {
  return {
    creatingDataset: DatasetsStore.selectCommunications(state).creatingDataset,
    workspaces: selectWorkspaces(state),
    currentWorkspace: selectCurrentWorkspaceName(state),
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

const typeOptions = (() => {
  const map: {
    [T in Dataset.DatasetType]: [{ label: string; value: T }, { order: number }]
  } = {
    path: [{ label: 'Path', value: 'path' }, { order: 0 }],
    raw: [{ label: 'Raw', value: 'raw' }, { order: 1 }],
    query: [{ label: 'Query', value: 'query' }, { order: 2 }],
  };
  return R.sortBy(([_, { order }]) => order, Object.values(map)).map(
    ([option]) => option
  );
})();

class DatasetCreation extends React.PureComponent<AllProps> {
  private initialSettings: Dataset.IDatasetCreationSettings = {
    description: '',
    name: '',
    tags: [],
    type: 'path',
    visibility: 'private',
    workspaceName: this.props.currentWorkspace,
  };

  public UNSAFE_componentWillMount() {
    this.props.resetCreatingDataset(undefined);
  }

  public render() {
    const { creatingDataset } = this.props;

    return (
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
                  <FieldWithTopLabel label="Type">
                    <SelectField
                      isMenuWithDynamicWidth={true}
                      name="type"
                      value={typeOptions.find(
                        typeOption => typeOption.value === values.type
                      )}
                      options={typeOptions}
                    />
                  </FieldWithTopLabel>
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
)(DatasetCreation);
