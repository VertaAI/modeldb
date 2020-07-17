import * as React from 'react';

import ClientSuggestion from 'shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import { IAttribute } from 'shared/models/Attribute';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';

import { IAttributesDiff } from '../../../../store/compareModels/compareModels';
import ComparableAttributeButton from './ComparableAttributeButton/ComparableAttributeButton';
import { IEntityInfo } from '../CompareModelsTable/CompareModelsTable';

interface ILocalProps {
  currentAttributesInfo: IEntityInfo<IAttribute[], IAttributesDiff>;
  allModelsAttributesInfo: Array<IEntityInfo<IAttribute[], IAttributesDiff>>;
  docLink?: string;
}

class ComparableAttributes extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      currentAttributesInfo,
      allModelsAttributesInfo,
      docLink,
    } = this.props;

    const comparableAttributeButtonProps = currentAttributesInfo.value.map((currentAttribute) => {
      const res: React.ComponentProps<typeof ComparableAttributeButton> = {
        currentAttribute: {
          attribute: currentAttribute,
          diff: currentAttributesInfo.diffInfo[currentAttribute.key],
          modelNumber: currentAttributesInfo.entityType,
        },
        modelsAttributesByKey: allModelsAttributesInfo
          .map((otherModelAttributes) => [otherModelAttributes, otherModelAttributes.value.find((x) => x.key === currentAttribute.key)] as const)
          .map(([{ entityType, diffInfo }, otherModelAttribute]) => otherModelAttribute && ({
            modelNumber: entityType,
            attribute: otherModelAttribute,
            diff: diffInfo[otherModelAttribute.key],
          })),
      };
      return res;
    });

    return (
      <div data-test="attributes">
        {comparableAttributeButtonProps.length !== 0 ? (
          <ScrollableContainer
            maxHeight={180}
            minRowCount={5}
            elementRowCount={comparableAttributeButtonProps.length}
            containerOffsetValue={12}
            children={
              <>
                {comparableAttributeButtonProps.map((comparableAttributeButtonProps, i) => (
                  <ComparableAttributeButton key={i} {...comparableAttributeButtonProps} />
                ))}
              </>
            }
          />
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

export default ComparableAttributes;
