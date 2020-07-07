import * as React from 'react';

import ModelRecord from 'shared/models/ModelRecord';
import vertaDocLinks from 'shared/utils/globalConstants/vertaDocLinks';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import { withScientificNotationOrRounded } from 'shared/utils/formatters/number';
import ClientSuggestion from 'shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';

import Section from '../shared/Section/Section';
import { RecordInfo } from '../shared/RecordInfo/RecordInfo';

interface ILocalProps {
  metrics: ModelRecord['metrics'];
}

const MetricsSection = ({ metrics }: ILocalProps) => {
  return (
    <Section iconType="metrics" title="Metrics">
      <div>
        {metrics.length > 0 ? (
          <ScrollableContainer
            maxHeight={180}
            containerOffsetValue={12}
            children={
              <>
                {metrics.map((metrics, key) => {
                  const maybeFormattedValue =
                    typeof metrics.value === 'number'
                      ? withScientificNotationOrRounded(metrics.value)
                      : metrics.value;
                  return (
                    <div key={key}>
                      <RecordInfo
                        label={metrics.key}
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
            fieldName={'metric'}
            clientMethod={'log_metric()'}
            link={vertaDocLinks.log_metric}
          />
        )}
      </div>
    </Section>
  );
};

export default MetricsSection;
