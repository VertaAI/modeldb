import React, { useCallback, useEffect } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { validateNotEmpty } from 'core/shared/utils/validators';
import Button from 'core/shared/view/elements/Button/Button';
import TextInputFieldWithTopLabel from 'core/shared/view/formComponents/formikFields/TextInputFieldWithTopLabel/TextInputFieldWithTopLabel';
import PresetFormik from 'core/shared/view/formComponents/presetComponents/PresetFormik/PresetFormik';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspace } from 'store/workspaces';

import { IRepository } from 'core/shared/models/Repository/Repository';
import { actions } from '../../store';
import { selectCommunications } from '../../store/selectors';
import styles from './RepositoryCreationForm.module.css';
import { useHistory } from 'react-router';
import routes from 'routes';
import { handleCustomErrorWithFallback } from 'core/shared/models/Error';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspace: selectCurrentWorkspace(state),
  creatingRepository: selectCommunications(state).creatingRepository,
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      createRepository: actions.createRepository,
      resetCreatingRepository: actions.createRepository.reset,
    },
    dispatch
  );
};

interface IRepositorySettings {
  name: IRepository['name'];
}

const initialValues: IRepositorySettings = {
  name: '',
};

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const RepositoryCreationForm: React.FC<AllProps> = ({
  createRepository,
  currentWorkspace,
  creatingRepository,
  resetCreatingRepository,
}) => {
  const onSubmit = useCallback(
    (values: IRepositorySettings) => {
      createRepository({
        repositorySettings: values,
        workspaceName: currentWorkspace.name,
      });
    },
    [createRepository, currentWorkspace.name]
  );

  const history = useHistory();

  useEffect(() => {
    if (creatingRepository.isSuccess) {
      history.push(
        routes.repositories.getRedirectPath({
          workspaceName: currentWorkspace.name,
        })
      );
    }
  }, [creatingRepository]);

  useEffect(() => {
    return () => {
      resetCreatingRepository();
    };
  }, []);

  return (
    <PresetFormik<IRepositorySettings>
      initialValues={initialValues}
      onSubmit={onSubmit}
    >
      {({ submitForm, isValid }) => (
        <div className={styles.root}>
          <TextInputFieldWithTopLabel
            name="name"
            validate={validateNotEmpty('repository name')}
            label="Repository name"
            size="medium"
            isRequired={true}
          />
          <div className={styles.button}>
            <Button
              onClick={submitForm}
              isLoading={creatingRepository.isRequesting}
              disabled={!isValid}
            >
              Create
            </Button>
          </div>
          {creatingRepository.error &&
            handleCustomErrorWithFallback(
              creatingRepository.error,
              {
                entityAlreadyExists: () => (
                  <InlineErrorView
                    error={'Repository with same name already exists'}
                  />
                ),
              },
              error => <InlineCommunicationError error={error} />
            )}
        </div>
      )}
    </PresetFormik>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(React.memo(RepositoryCreationForm));
