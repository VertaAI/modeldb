import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { IArtifactWithPath } from 'core/shared/models/Artifact';
import { artifactErrorMessages } from 'core/shared/utils/customErrorMessages';
import {
  ICommunication,
  initialCommunication,
} from 'core/shared/utils/redux/communication';
import Button from 'core/shared/view/elements/Button/Button';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import {
  downloadArtifact,
  selectDownloadingArtifact,
  EntityType,
  reset,
} from 'store/artifactManager';
import { IApplicationState } from 'store/store';

import styles from './DownloadArtifactButton.module.css';

interface ILocalProps {
  entityId: string;
  entityType: EntityType;
  artifact: IArtifactWithPath;
  isShowErrorIfExist: boolean;
}

interface IPropsFromState {
  downloadingArtifact: ICommunication;
}

interface IActionProps {
  reset: typeof reset;
  downloadArtifact: typeof downloadArtifact;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class DownloadArtifactButton extends React.PureComponent<AllProps> {
  public componentWillUnmount() {
    this.props.reset();
  }

  public render() {
    const { downloadingArtifact, isShowErrorIfExist, artifact } = this.props;
    if (R.equals(downloadingArtifact, initialCommunication)) {
      return (
        <div className={styles.root}>
          <Button onClick={this.downloadArtifact}>Download Artifact</Button>
        </div>
      );
    }
    if (downloadingArtifact.isRequesting) {
      return (
        <div className={styles.preloader}>
          <Preloader variant="dots" />
        </div>
      );
    }
    if (downloadingArtifact.error) {
      return (
        <div className={styles.root}>
          <Button disabled={true} onClick={this.downloadArtifact}>
            Download Failed
          </Button>
          {isShowErrorIfExist && (
            <div className={styles.url_call_message}>
              <InlineCommunicationError
                error={downloadingArtifact.error}
                customMessage={artifactErrorMessages.artifact_download}
              />
            </div>
          )}
        </div>
      );
    }
    return (
      <div className={styles.root}>
        <Button disabled={true}>Downloaded</Button>
        <div className={styles.url_call_message}>
          <div className={styles.success_message}>
            Downloaded <Icon type="check-circle" />
          </div>
        </div>
      </div>
    );
  }

  @bind
  private downloadArtifact() {
    const { entityId, entityType, artifact } = this.props;
    this.props.downloadArtifact(entityType, entityId, artifact);
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    downloadingArtifact: selectDownloadingArtifact(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators({ downloadArtifact, reset }, dispatch);
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DownloadArtifactButton);
