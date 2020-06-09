import React, { useCallback } from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';

import routes from 'shared/routes';
import { validateNotEmpty } from 'shared/utils/validators';
import Button from 'shared/view/elements/Button/Button';
import TextInputFieldWithTopLabel from 'shared/view/formComponents/formikFields/TextInputFieldWithTopLabel/TextInputFieldWithTopLabel';
import PresetFormik from 'shared/view/formComponents/presetComponents/PresetFormik/PresetFormik';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspace } from 'features/workspaces/store';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import { IRepository } from 'shared/models/Versioning/Repository';

import styles from './RepositoryCreationForm.module.css';
import { useCreateRepositoryMutation } from '../../store/createRepository/useCreateRepository';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspace: selectCurrentWorkspace(state),
});

interface IRepositorySettings {
  name: IRepository['name'];
  isOrgPublic?: boolean;
}

type AllProps = ReturnType<typeof mapStateToProps>;

const RepositoryCreationForm: React.FC<AllProps> = ({ currentWorkspace }) => {
  const initialSettings: IRepositorySettings = React.useMemo(() => {
    return {
      name: '',
      isOrgPublic: false,
    };
  }, []);

  const history = useHistory();
  const {
    createRepository,
    communication: creatingRepository,
  } = useCreateRepositoryMutation();

  const onSubmit = useCallback(
    (values: IRepositorySettings) => {
      createRepository(
        {
          name: values.name,
          isOrgPublic: Boolean(values.isOrgPublic),
          workspaceName: currentWorkspace.name,
        },
        () => {
          history.push(
            routes.repositories.getRedirectPath({
              workspaceName: currentWorkspace.name,
            })
          );
        }
      );
    },
    [createRepository, currentWorkspace.name]
  );

  return (
    <PresetFormik<IRepositorySettings>
      initialValues={initialSettings}
      onSubmit={onSubmit}
    >
      {({ submitForm, values, setFieldValue, isValid }) => (
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
          {creatingRepository.error && (
            <InlineCommunicationError
              withoutErrorCode={true}
              error={creatingRepository.error}
            />
          )}
        </div>
      )}
    </PresetFormik>
  );
};

export default connect(mapStateToProps)(React.memo(RepositoryCreationForm));
