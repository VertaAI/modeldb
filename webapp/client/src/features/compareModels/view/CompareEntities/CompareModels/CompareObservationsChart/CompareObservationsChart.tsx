import * as React from 'react';
import cn from 'classnames';
import * as R from 'ramda';

import {
  IObservation,
  groupObservationsByAttributeKey,
  IGroupedObservationsByAttributeKey,
} from 'shared/models/Observation';
import { PageCard, PageHeader } from 'shared/view/elements/PageComponents';
import { getObservationLineData } from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/ObservationsModelPage/observationsChartHelpers';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import ObservationButton from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/ObservationButton/ObservationButton';
import { Icon } from 'shared/view/elements/Icon/Icon';
import ObservationsChart from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/ObservationsModelPage/ObservationsChart/ObservationsChart';
import Legend, {
  LegendItems,
  IObservationLineDataWithColor,
} from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/ObservationsModelPage/ObservationsChart/Legend/Legend';
import { useXAxisTypeSelect } from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/ObservationsModelPage/ObservationsModelPage';

import styles from './CompareObservationsChart.module.css';
import { ComparedMultipleModels } from 'features/compareModels/store/compareModels/compareModels';

interface IModelObservations {
  modelNumber: number;
  observations: IObservation[];
}

interface ILocalProps {
  modelsObservations: ComparedMultipleModels;
}

type IObservationSelectionByModel = Record<
  IModelObservations['modelNumber'],
  Record<string, boolean>
>;

export const CompareObservartionsChart = ({
  modelsObservations,
}: ILocalProps) => {
  const [selectedObservationsByModel, setSelectedObservations] = React.useState<
    IObservationSelectionByModel
  >(() => getInitialSelectionByModel({ modelsObservations }));
  const toggleObservationSelection = (
    modelNumber: number,
    attributeKey: string
  ) => {
    setSelectedObservations({
      ...selectedObservationsByModel,
      [modelNumber]: {
        ...selectedObservationsByModel[modelNumber],
        [attributeKey]: !selectedObservationsByModel[modelNumber][attributeKey],
      },
    });
  };

  const { xAxisType, xAxisTypeSelect } = useXAxisTypeSelect(
    modelsObservations.flatMap(({ observations }) => observations)
  );

  const modelsGroupedObservations = modelsObservations.map(
    ({ modelNumber, observations }) => ({
      modelNumber,
      groupedObservations: groupObservationsByAttributeKey(observations),
    })
  );
  const chartData = modelsGroupedObservations.flatMap(
    ({ modelNumber, groupedObservations }) =>
      getObservationLineData(
        groupedObservations,
        selectedObservationsByModel[modelNumber]
      ).map(d => ({ ...d, modelNumber }))
  );

  return modelsGroupedObservations.some(
    ({ groupedObservations }) => Object.keys(groupedObservations).length > 0
  ) ? (
    <PageCard>
      <div className={styles.root}>
        <PageHeader
          title="Observations chart"
          size="small"
          withoutSeparator={true}
        />
      </div>
      {xAxisTypeSelect}
      <div className={styles.observations_wrapper}>
        <div className={styles.observations}>
          {modelsGroupedObservations.map(
            ({ modelNumber, groupedObservations }) => (
              <GroupedObservations
                title={`Model ${modelNumber}`}
                groupedObservations={groupedObservations}
                onToggleObservationSelection={attributeKey =>
                  toggleObservationSelection(modelNumber, attributeKey)
                }
                selectedObservations={selectedObservationsByModel[modelNumber]}
              />
            )
          )}
        </div>
        <div className={styles.observations_chart_container}>
          <ObservationsChart
            data={chartData}
            xAxisType={xAxisType}
            renderLegend={({
              data,
            }: {
              data: (IObservationLineDataWithColor & { modelNumber: number })[];
            }) => {
              const modelsChartData = R.groupBy(
                ({ modelNumber }) => String(modelNumber),
                data
              );

              return (
                <Legend
                  data={data}
                  renderLegendItems={() => (
                    <>
                      {R.sortBy(
                        ([modelNumber]) => modelNumber,
                        R.toPairs(modelsChartData)
                      ).map(([modelNumber, data]) => (
                        <div>
                          Model {modelNumber}
                          <LegendItems data={data} />
                        </div>
                      ))}
                    </>
                  )}
                />
              );
            }}
          />
        </div>
      </div>
    </PageCard>
  ) : null;
};

const GroupedObservations = ({
  groupedObservations,
  selectedObservations,
  title,
  onToggleObservationSelection,
}: {
  title: string;
  groupedObservations: IGroupedObservationsByAttributeKey;
  selectedObservations: Record<string, boolean>;
  onToggleObservationSelection: (attributeKey: string) => void;
}) => {
  if (Object.keys(groupedObservations).length === 0) {
    return null;
  }

  return (
    <div>
      <div className={styles.grouped_observations_title}>{title}</div>
      <ScrollableContainer
        maxHeight={480}
        containerOffsetValue={12}
        children={
          <>
            {Object.entries(groupedObservations).map(
              ([attributeKey, values]) => {
                return (
                  <div
                    className={styles.attribute_container}
                    key={attributeKey}
                  >
                    <ObservationButton
                      values={values}
                      attributeKey={attributeKey}
                    />
                    <Icon
                      type={'arrow-right-circle'}
                      className={cn(
                        styles.icon,
                        selectedObservations[attributeKey]
                          ? styles.selected_option_icon
                          : styles.option_icon
                      )}
                      key={attributeKey}
                      onClick={() => onToggleObservationSelection(attributeKey)}
                    />
                  </div>
                );
              }
            )}
          </>
        }
      />
    </div>
  );
};

function getInitialSelectionByModel({
  modelsObservations,
}: {
  modelsObservations: IModelObservations[];
}): IObservationSelectionByModel {
  const getInitialSelection = (observations: IObservation[]) => {
    const observationAttrs = Object.keys(
      groupObservationsByAttributeKey(observations)
    );
    const initialSelectedObservationAttrs = observationAttrs.slice(0, 3);

    return Object.fromEntries(
      observationAttrs.map(
        attr => [attr, initialSelectedObservationAttrs.includes(attr)] as const
      )
    );
  };

  return R.fromPairs(
    modelsObservations.map(({ modelNumber, observations }) => [
      modelNumber,
      getInitialSelection(observations),
    ])
  );
}
