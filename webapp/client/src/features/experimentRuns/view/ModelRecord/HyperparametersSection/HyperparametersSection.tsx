import * as React from 'react';

import ModelRecord from 'core/shared/models/ModelRecord';
import vertaDocLinks from 'core/shared/utils/globalConstants/vertaDocLinks';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import ClientSuggestion from 'core/shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';

import Section from '../shared/Section/Section';
import { RecordInfo } from '../shared/RecordInfo/RecordInfo';

interface ILocalProps {
  hyperparameters: ModelRecord['hyperparameters'];
}

const HyperparametersSection = ({ hyperparameters }: ILocalProps) => {
  return (
    <Section iconType="hyperpameters" title="Hyperparameters">
      <div>
        {hyperparameters.length > 0 ? (
          <ScrollableContainer
            maxHeight={180}
            containerOffsetValue={12}
            children={
              <>
                {hyperparameters.map((hyperparameter, key) => {
                  const maybeFormattedValue =
                    typeof hyperparameter.value === 'number'
                      ? withScientificNotationOrRounded(hyperparameter.value)
                      : hyperparameter.value;
                  return (
                    <div key={key}>
                      <RecordInfo
                        label={hyperparameter.key}
                        valueTitle={String(maybeFormattedValue)}
                      >
                        {maybeFormattedValue}
                      </RecordInfo>
                    </div>
                  );
                })}
              </>
            }
          />
        ) : (
          <ClientSuggestion
            fieldName={'hyperparameter'}
            clientMethod={'log_hyperparameters()'}
            link={vertaDocLinks.log_hyperparameters}
          />
        )}
      </div>
    </Section>
  );
};

export default HyperparametersSection;
