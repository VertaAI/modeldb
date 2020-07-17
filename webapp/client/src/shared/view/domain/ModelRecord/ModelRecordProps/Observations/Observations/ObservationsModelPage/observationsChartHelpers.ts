import {
  IObservationsValues,
  IGroupedObservationsByAttributeKey,
} from 'shared/models/Observation';

export interface IObservationLineData {
  lineIndex: string;
  values: IObservationsValues[];
}

export function getObservationLineData(
  groupedObservationsByAttributeKeys: IGroupedObservationsByAttributeKey,
  selectedObservations: Record<string, boolean>
): IObservationLineData[] {
  return Object.entries(groupedObservationsByAttributeKeys)
    .filter(([key]) => selectedObservations[key])
    .map(([key, values]) => ({
      values,
      lineIndex: key,
    }));
}
