import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import Pile from 'core/shared/view/elements/PileWithPopup/Pile/Pile';
import { EntityType } from 'features/artifactManager/store';
import { IApplicationState } from 'store/store';

import DownloadArtifactButton from 'features/artifactManager/view/DownloadArtifactButton/DownloadArtifactButton';
import { checkArtifactWithPath } from 'core/shared/models/Artifact';
import { IArtifactCodeVersion } from 'core/shared/models/CodeVersion';
import PilePopup from 'core/shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'core/shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';

interface IPropsFromState {}

interface IActionProps {}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

export const ArtifactCodeVersionFields = ({
  getAdditionalValueClassname = () => undefined,
  additionalFields,
  artifactCodeVersion,
}: {
  getAdditionalValueClassname?: (t: string) => string | undefined;
  additionalFields?: Array<{ label: string; content: React.ReactNode }>;
  artifactCodeVersion: IArtifactCodeVersion;
}) => (
  <PilePopup.Fields>
    {Field => (
      <>
        {(additionalFields || []).map(field => (
          <Field label={field.label}>{field.content}</Field>
        ))}
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
    additionalFields?: Array<{ label: string; content: React.ReactNode }>;
    onClose(): void;
  }) => {
    const { artifactCodeVersion, entityId, entityType, onClose } = props;
    return (
      <PilePopup
        title="Code Version"
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

export const ArtifactCodeVersionPile = ({
  additionalClassname,
  onClick,
  label,
}: {
  additionalClassname: string | undefined;
  label?: string;
  onClick: () => void;
}) => (
  <Pile
    title="view artifact"
    dataTest="artifact"
    labelDataTest="artifact-key"
    label={label || 'Code Version'}
    iconType="codepen"
    additionalClassname={additionalClassname}
    onClick={onClick}
  />
);

interface ILocalProps {
  artifactCodeVersion: IArtifactCodeVersion;
  entityId: string;
  entityType: EntityType;
  additionalButtonClassname?: string;
  popupProps?: {
    additionalFields?: Array<{ label: string; content: React.ReactNode }>;
  };
  pileProps?: {
    label: string;
  };
}

class ArtifactCodeVersion extends React.PureComponent<AllProps> {
  public render() {
    const {
      artifactCodeVersion,
      entityId,
      entityType,
      additionalButtonClassname,
      pileProps,
    } = this.props;
    return (
      <PileWithPopup
        pileComponent={({ showPopup }) => (
          <ArtifactCodeVersionPile
            label={pileProps ? pileProps.label : undefined}
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
