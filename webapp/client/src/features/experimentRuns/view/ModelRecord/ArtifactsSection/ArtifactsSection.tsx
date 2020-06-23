import * as React from 'react';

import ModelRecord from 'shared/models/ModelRecord';
import ClientSuggestion from 'shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import vertaDocLinks from 'shared/utils/globalConstants/vertaDocLinks';

import Section from '../shared/Section/Section';
import Artifacts from '../shared/Artifacts/Artifacts';

interface ILocalProps {
  modelRecordId: ModelRecord['id'];
  artifacts: ModelRecord['artifacts'];
  allowedActions: ModelRecord['allowedActions'];
}

const ArtifactsSection = ({
  artifacts,
  modelRecordId,
  allowedActions,
}: ILocalProps) => {
  return (
    <Section iconType="artifacts" title="Artifacts">
      {artifacts.length > 0 ? (
        <Artifacts
          allowedActions={allowedActions}
          artifacts={artifacts}
          modelRecordId={modelRecordId}
        />
      ) : (
        <ClientSuggestion
          fieldName={'artifact'}
          clientMethod={'log_artifact()'}
          link={vertaDocLinks.log_artifact}
        />
      )}
    </Section>
  );
};

export default ArtifactsSection;
