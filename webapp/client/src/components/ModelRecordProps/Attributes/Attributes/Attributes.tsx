import * as React from 'react';

import { IAttribute } from 'core/shared/models/Attribute';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import ClientSuggestion from '../../shared/ClientSuggestion/ClientSuggestion';
import AttributeButton from '../AttributeButton/AttributeButton';

import styles from '../../shared/sharedStyles.module.css';

type PillSize = 'small' | 'medium';

interface ILocalProps {
  attributes: IAttribute[];
  maxHeight?: number;
  docLink?: string;
  pillSize?: PillSize;
  getAttributeClassname?: (key: string) => string | undefined;
}

class Attributes extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      attributes,
      docLink,
      maxHeight,
      pillSize,
      getAttributeClassname = () => undefined,
    } = this.props;

    return (
      <div className={styles.root} data-test="attributes">
        {attributes.length !== 0 ? (
          <div
            className={
              pillSize === 'medium' ? styles.record_pills_container : ''
            }
          >
            <ScrollableContainer
              maxHeight={maxHeight || 180}
              containerOffsetValue={12}
              children={
                <>
                  {attributes.map((attribute: IAttribute, i: number) => (
                    <AttributeButton
                      key={i}
                      additionalClassname={getAttributeClassname(attribute.key)}
                      attribute={attribute}
                    />
                  ))}
                </>
              }
            />
          </div>
        ) : (
          docLink && (
            <ClientSuggestion
              fieldName={'attribute'}
              clientMethod={'log_attribute()'}
              link={docLink}
            />
          )
        )}
      </div>
    );
  }
}

export default Attributes;
