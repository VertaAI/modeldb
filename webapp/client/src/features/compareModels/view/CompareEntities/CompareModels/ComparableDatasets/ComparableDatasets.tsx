import * as React from 'react';

import ClientSuggestion from 'shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import { IArtifact } from 'shared/models/Artifact';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';

import ComparableDatasetButton from './ComparableDatasetButton/ComparableDatasetButton';
import { IDatasetsDiff } from '../../../../store/compareModels/compareModels';
import { IEntityInfo } from '../CompareModelsTable/CompareModelsTable';

interface ILocalProps {
  currentDatasetsInfo: IEntityInfo<IArtifact[], IDatasetsDiff>;
  allModelsDatasetsInfo: Array<IEntityInfo<IArtifact[], IDatasetsDiff>>;
  docLink?: string;
}

class ComparableDatasets extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      allModelsDatasetsInfo,
      currentDatasetsInfo,
      docLink,
    } = this.props;

    const comparableDatasetButtonProps = currentDatasetsInfo.value.map((currentDataset) => {
      const res = {
        currentDataset: {
          dataset: currentDataset,
          diff: currentDatasetsInfo.diffInfo[currentDataset.key],
          modelNumber: currentDatasetsInfo.entityType,
        },
        modelsDatasetsByKey: allModelsDatasetsInfo
          .map((otherModelDatasets) => [otherModelDatasets, otherModelDatasets.value.find((x) => x.key === currentDataset.key)] as const)
          .map(([{ entityType, diffInfo }, otherModelDataset]) => otherModelDataset && ({
            modelNumber: entityType,
            dataset: otherModelDataset,
            diff: diffInfo[otherModelDataset.key],
          })),
      };
      return res;
    });

    return (
      <div data-test="datasets">
        {comparableDatasetButtonProps.length !== 0 ? (
          <ScrollableContainer
            maxHeight={180}
            minRowCount={5}
            elementRowCount={comparableDatasetButtonProps.length}
            containerOffsetValue={12}
            children={
              <>
                {comparableDatasetButtonProps.map((props) => (
                  <ComparableDatasetButton
                    key={props.currentDataset.dataset.key}
                    {...props}
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
