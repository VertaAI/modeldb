import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import withProps from 'core/shared/utils/react/withProps';
import Pile from 'core/shared/view/elements/PileWithPopup/Pile/Pile';
import { EntityType } from 'store/artifactManager';
import { IApplicationState } from 'store/store';

import DownloadArtifactButton from 'components/ModelRecordProps/Artifacts/DownloadArtifactButton/DownloadArtifactButton';
import { checkArtifactWithPath } from 'core/shared/models/Artifact';
import { IArtifactCodeVersion } from 'core/shared/models/CodeVersion';
import PilePopup from 'core/shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'core/shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';

interface ILocalProps {
  artifactCodeVersion: IArtifactCodeVersion;
  entityId: string;
  entityType: EntityType;
  additionalButtonClassname?: string;
}

interface IPropsFromState {}

interface IActionProps {}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

export const ArtifactCodeVersionPile = withProps(Pile)({
  title: 'view artifact',
  dataTest: 'artifact',
  labelDataTest: 'artifact-key',
  label: 'Code Version',
  iconType: 'codepen',
});

export const ArtifactCodeVersionFields = ({
  getAdditionalValueClassname = () => undefined,
  artifactCodeVersion,
}: {
  getAdditionalValueClassname?: (t: string) => string | undefined;
  artifactCodeVersion: IArtifactCodeVersion;
}) => (
  <PilePopup.Fields>
    {Field => (
      <>
        <Field label="Key">
          <span className={getAdditionalValueClassname('key')}>
            {artifactCodeVersion.data.key}
          </span>
        </Field>
        <Field label="Type">
          <span className={getAdditionalValueClassname('type')}>
            {artifactCodeVersion.data.type}
          </span>
        </Field>
        <Field label="Path">
          <span className={getAdditionalValueClassname('path')}>
            {artifactCodeVersion.data.path}
          </span>
        </Field>
      </>
    )}
  </PilePopup.Fields>
);

const ArtifactCodeVersionPopup = React.memo(
  (props: {
    artifactCodeVersion: IArtifactCodeVersion;
    entityId: string;
    entityType: EntityType;
    onClose(): void;
  }) => {
    const { artifactCodeVersion, entityId, entityType, onClose } = props;
    return (
      <PilePopup
        title="Artifact"
        titleIcon="codepen"
        contentLabel="artifact-action"
        onRequestClose={onClose}
        isOpen={true}
      >
        <ArtifactCodeVersionFields artifactCodeVersion={artifactCodeVersion} />
        {checkArtifactWithPath(artifactCodeVersion.data) ? (
          <PilePopup.Actions>
            <DownloadArtifactButton
              isShowErrorIfExist={true}
              artifact={artifactCodeVersion.data}
              entityId={entityId}
              entityType={entityType}
            />
          </PilePopup.Actions>
        ) : null}
      </PilePopup>
    );
  }
);

class ArtifactCodeVersion extends React.PureComponent<AllProps> {
  public render() {
    const {
      artifactCodeVersion,
      entityId,
      entityType,
      additionalButtonClassname,
    } = this.props;
    return (
      <PileWithPopup
        pileComponent={({ showPopup }) => (
          <ArtifactCodeVersionPile
            additionalClassname={additionalButtonClassname}
            onClick={showPopup}
          />
        )}
        popupComponent={({ isOpen, closePopup }) =>
          isOpen ? (
            <ArtifactCodeVersionPopup
              artifactCodeVersion={artifactCodeVersion}
              entityId={entityId}
              entityType={entityType}
              onClose={closePopup}
            />
          ) : null
        }
      />
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {};
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators({}, dispatch);
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ArtifactCodeVersion);
