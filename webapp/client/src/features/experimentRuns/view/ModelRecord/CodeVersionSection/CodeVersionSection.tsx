import * as React from 'react';

import ModelRecord from 'core/shared/models/ModelRecord';
import CodeVersions from 'core/shared/view/domain/ModelRecord/ModelRecordProps/CodeVersions/CodeVersions';
import ClientSuggestion from 'core/shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import vertaDocLinks from 'core/shared/utils/globalConstants/vertaDocLinks';

import Section from '../shared/Section/Section';

const CodeVersionSection = ({
  id,
  codeVersion,
  codeVersionsFromBlob,
  versionedInputs,
}: Pick<
  ModelRecord,
  'id' | 'codeVersion' | 'codeVersionsFromBlob' | 'versionedInputs'
>) => {
  return (
    <Section iconType="code" title="Code Version">
      {!codeVersion && !codeVersionsFromBlob ? (
        <ClientSuggestion
          fieldName={'Code Version'}
          clientMethod={'log_code()'}
          link={vertaDocLinks.log_code}
        />
      ) : (
        <CodeVersions
          versionedInputs={versionedInputs}
          experimentRunId={id}
          codeVersion={codeVersion}
          codeVersionsFromBlob={codeVersionsFromBlob}
        />
      )}
    </Section>
  );
};

export default CodeVersionSection;
