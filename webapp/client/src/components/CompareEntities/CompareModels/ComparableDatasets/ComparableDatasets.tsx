import * as React from 'react';

import ClientSuggestion from 'components/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import { IArtifact } from 'core/shared/models/Artifact';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import { IModelsDifferentProps, EntityType } from 'store/compareEntities';

import ComparableDatasetButton from './ComparableDatasetButton/ComparableDatasetButton';

interface ILocalProps {
  entity1Datasets: IArtifact[];
  entity2Datasets: IArtifact[];
  entityType: EntityType;
  diffInfo: IModelsDifferentProps['datasets'];
  docLink?: string;
}

export interface IComparedDataset {
  dataset: IArtifact;
  otherEntityDataset?: IArtifact;
  isDifferent: boolean;
}

class ComparableDatasets extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      entity1Datasets,
      entity2Datasets,
      diffInfo,
      entityType,
      docLink,
    } = this.props;

    const comparedDatasets = (() => {
      if (entityType === EntityType.entity1) {
        return entity1Datasets.map<IComparedDataset>(dataset => ({
          dataset,
          otherEntityDataset: entity2Datasets.find(
            dataset2 => dataset.key === dataset2.key
          ),
          isDifferent: diffInfo[dataset.key],
        }));
      }
      return entity2Datasets.map<IComparedDataset>(dataset => ({
        dataset,
        otherEntityDataset: entity1Datasets.find(
          dataset => dataset.key === dataset.key
        ),
        isDifferent: diffInfo[dataset.key],
      }));
    })();

    return (
      <div data-test="datasets">
        {comparedDatasets.length !== 0 ? (
          <ScrollableContainer
            maxHeight={180}
            minRowCount={5}
            elementRowCount={comparedDatasets.length}
            containerOffsetValue={12}
            children={
              <>
                {comparedDatasets.map(comparedDataset => (
                  <ComparableDatasetButton
                    key={comparedDataset.dataset.key}
                    comparedDataset={comparedDataset}
                    entityType={entityType}
                  />
                ))}
              </>
            }
          />
        ) : (
          docLink && (
            <ClientSuggestion
              fieldName={'dataset'}
              clientMethod={'log_dataset_version()'}
              link={docLink}
            />
          )
        )}
      </div>
    );
  }
}

export default ComparableDatasets;
