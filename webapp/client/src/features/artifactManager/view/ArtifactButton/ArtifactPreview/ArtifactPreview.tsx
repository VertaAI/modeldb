import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { IArtifact } from 'shared/models/Artifact';
import { artifactErrorMessages } from 'shared/utils/customErrorMessages';
import { ICommunication } from 'shared/utils/redux/communication';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import { Icon } from 'shared/view/elements/Icon/Icon';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import {
  loadArtifactPreview,
  selectLoadingArtifactPreview,
  selectArtifactPreview,
  EntityType,
  ArtifactPreviewFileExtension,
  ArtifactPreviewFileExtensions,
} from 'features/artifactManager/store';
import { getArtifactPreviewFileExtension } from 'features/artifactManager/store/helpers';
import { IApplicationState } from 'setup/store/store';

import styles from './ArtifactPreview.module.css';

interface ILocalProps {
  entityType: EntityType;
  entityId: string;
  artifact: IArtifact;
  isShowErrorIfExist: boolean;
  onClose(): void;
}

interface IPropsFromState {
  preview: string | null;
  loadingArtifactPreview: ICommunication;
}

interface IActionProps {
  loadArtifactPreview: typeof loadArtifactPreview;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class ArtifactPreview extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.props.loadArtifactPreview(
      this.props.entityType,
      this.props.entityId,
      this.props.artifact
    );
  }

  public render() {
    const {
      loadingArtifactPreview,
      preview,
      artifact,
      isShowErrorIfExist,
      onClose,
    } = this.props;

    if (loadingArtifactPreview.isRequesting) {
      return (
        <div className={styles.root}>
          <div className={styles.preloader}>
            <Preloader variant="dots" />
          </div>
        </div>
      );
    }
    if (isShowErrorIfExist && loadingArtifactPreview.error) {
      return (
        <InlineCommunicationError
          error={loadingArtifactPreview.error}
          customMessage={artifactErrorMessages.artifact_preview}
        />
      );
    }
    if (!preview) {
      return null;
    }
    return (
      <div className={styles.root}>
        <div className={styles.content}>
          <Icon type="close" className={styles.close} onClick={onClose} />
          {this.getView(preview, getArtifactPreviewFileExtension(artifact))}
        </div>
      </div>
    );
  }

  @bind
  private getView(preview: string, type: ArtifactPreviewFileExtension | null) {
    const viewsByType: Record<
      ArtifactPreviewFileExtension,
      () => React.ReactNode
    > = {
      [ArtifactPreviewFileExtensions.text]: () => (
        <div className={styles.preview_txt}>{preview}</div>
      ),
      [ArtifactPreviewFileExtensions.txt]: () => (
        <div className={styles.preview_txt}>{preview}</div>
      ),
      [ArtifactPreviewFileExtensions.gif]: () => (
        <img className={styles.preview_img} src={preview} alt="preview" />
      ),
      [ArtifactPreviewFileExtensions.jpeg]: () => (
        <img className={styles.preview_img} src={preview} alt="preview" />
      ),
      [ArtifactPreviewFileExtensions.png]: () => (
        <img className={styles.preview_img} src={preview} alt="preview" />
      ),
      [ArtifactPreviewFileExtensions.json]: () => {
        const formattedPreview = JSON.stringify(JSON.parse(preview), null, 4);
        return <div className={styles.preview_json}>{formattedPreview}</div>;
      },
    };
    return type ? (
      viewsByType[type]()
    ) : (
      <div>preview for such artifact is not supported</div>
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    loadingArtifactPreview: selectLoadingArtifactPreview(state),
    preview: selectArtifactPreview(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      loadArtifactPreview,
    },
    dispatch
  );
};

export default connect(mapStateToProps, mapDispatchToProps)(ArtifactPreview);
