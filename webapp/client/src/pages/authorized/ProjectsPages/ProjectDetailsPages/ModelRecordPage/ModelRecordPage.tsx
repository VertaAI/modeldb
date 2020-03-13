import { bind } from 'decko';
import * as React from 'react';
import { RouteComponentProps } from 'react-router';

import ModelRecord from 'components/ModelRecord/ModelRecord';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import routes, { GetRouteParams } from 'routes';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import styles from './ModelRecordPage.module.css';

type IUrlProps = GetRouteParams<typeof routes.modelRecord>;

type AllProps = RouteComponentProps<IUrlProps>;

interface ILocalState {
  isShowNotFoundPage: boolean;
  entityError: any;
}

class ModelRecordPage extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isShowNotFoundPage: false,
    entityError: undefined,
  };

  public render() {
    const {
      match: {
        params: { modelRecordId, projectId },
      },
    } = this.props;
    const { isShowNotFoundPage, entityError } = this.state;

    return isShowNotFoundPage ? (
      <AuthorizedLayout>
        <PageCommunicationError error={entityError} />
      </AuthorizedLayout>
    ) : (
      <ProjectsPagesLayout>
        <div className={styles.root}>
          <div className={styles.modelRecord}>
            <ModelRecord
              projectId={projectId}
              id={modelRecordId}
              onShowNotFoundPage={this.showNotFoundPage}
              onDelete={this.onDelete}
            />
          </div>
        </div>
      </ProjectsPagesLayout>
    );
  }

  @bind
  private showNotFoundPage(error: any) {
    this.setState({ isShowNotFoundPage: true, entityError: error });
  }

  @bind
  private onDelete() {
    this.props.history.replace(
      routes.experimentRuns.getRedirectPathWithCurrentWorkspace({
        projectId: this.props.match.params.projectId,
      })
    );
  }
}

export default ModelRecordPage;
