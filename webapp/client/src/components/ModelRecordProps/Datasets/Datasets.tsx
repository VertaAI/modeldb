import * as React from 'react';

import ClientSuggestion from 'components/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import { IArtifact } from 'core/shared/models/Artifact';

import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import ArtifactButton from '../Artifacts/ArtifactButton/ArtifactButton';

import styles from '../shared/sharedStyles.module.css';

type PillSize = 'small' | 'medium';

interface ILocalProps {
  modelId: string;
  datasets: IArtifact[];
  maxHeight?: number;
  size?: PillSize;
  getDatasetsClassname?: (key: string) => string | undefined;
  docLink?: string;
}

class Datasets extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      datasets,
      modelId,
      size,
      maxHeight,
      getDatasetsClassname = () => undefined,
      docLink,
    } = this.props;
    return (
      <div>
        {datasets.length !== 0 ? (
          <div
            className={size === 'medium' ? styles.record_pills_container : ''}
          >
            <ScrollableContainer
              maxHeight={maxHeight || 180}
              containerOffsetValue={12}
              children={
                <>
                  {datasets.map((dataset: IArtifact, i: number) => (
                    <ArtifactButton
                      key={i}
                      entityType="experimentRun"
                      entityId={modelId}
                      additionalClassname={getDatasetsClassname(dataset.key)}
                      artifact={dataset}
                    />
                  ))}
                </>
              }
            />
          </div>
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

export default Datasets;
