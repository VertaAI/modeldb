import * as React from 'react';

import styles from '../ObservationsChart.module.css';
import { IObservationLineData } from '../../observationsChartHelpers';

interface ILocalProps {
  data: IObservationLineDataWithColor[];
  renderLegendItems?: (settings: { data: IObservationLineData[] }) => void;
}

export interface IObservationLineDataWithColor extends IObservationLineData {
  color: string;
}

const Legend = ({ data, renderLegendItems }: ILocalProps) => {
  return (
    <div className={styles.legendBlock}>
      <div className={styles.legendTitle}>Observations:</div>
      <div className={styles.legendContent}>
        {renderLegendItems ? (
          renderLegendItems({ data })
        ) : (
          <div className={styles.legendItems}>
            {data.map(({ lineIndex, color }, i) => (
              <div className={styles.legendItem}>
                <div className={styles.legendItem__label}>{lineIndex}</div>
                <div
                  className={styles.legendItem__color}
                  style={{ backgroundColor: color }}
                ></div>
              </div>
            ))}
          </div>
        )}
        <div />
      </div>
    </div>
  );
};

export const LegendItems = ({
  data,
}: {
  data: IObservationLineDataWithColor[];
}) => {
  return (
    <div className={styles.legendItems}>
      {data.map(({ lineIndex, color }, i) => (
        <div className={styles.legendItem}>
          <div className={styles.legendItem__label}>{lineIndex}</div>
          <div
            className={styles.legendItem__color}
            style={{ backgroundColor: color }}
          ></div>
        </div>
      ))}
    </div>
  );
};

export default Legend;
