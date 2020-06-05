import * as React from 'react';

import { IAttribute } from 'core/shared/models/Attribute';
import ClientSuggestion from 'core/shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import Section from '../shared/Section/Section';
import vertaDocLinks from 'core/shared/utils/globalConstants/vertaDocLinks';
import PileWithActions from 'core/shared/view/elements/PileWithActions/PileWithActions';
import { useInfoAction } from './InfoAction/InfoAction';

import styles from './AttributesSection.module.css';

const AttributesSection = ({ attributes }: { attributes: IAttribute[] }) => {
  return (
    <Section iconType="attributes" title="Attributes">
      <div className={styles.root}>
        {attributes.length > 0 ? (
          <ScrollableContainer maxHeight={180} containerOffsetValue={12}>
            <div className={styles.attributes}>
              {attributes.map((attribute: any) => (
                <Attribute attribute={attribute} />
              ))}
            </div>
          </ScrollableContainer>
        ) : (
          <ClientSuggestion
            fieldName={'attribute'}
            clientMethod={'log_attribute()'}
            link={vertaDocLinks.log_attribute}
          />
        )}
      </div>
    </Section>
  );
};

const Attribute = ({ attribute }: { attribute: IAttribute }) => {
  const infoAction = useInfoAction({ attribute, popupIconType: 'cube' });

  return (
    <PileWithActions
      pile={{
        iconType: 'cube',
        label: attribute.key,
        title: attribute.key,
      }}
      actions={[infoAction.content]}
      isShowPreloader={false}
    />
  );
};

export default AttributesSection;
