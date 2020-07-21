import { DatasetVerisonEntityType } from 'features/compareDatasets/store/compareDatasets';
import styles from './DiffHighlight.module.css';

export const getDiffValueBgClassname = (
  entityType: DatasetVerisonEntityType,
  isDifferent: boolean
) => {
  return isDifferent ? styles[`highlightBg_${entityType}`] : undefined;
};

export const getDiffValueBorderClassname = (
  entityType: DatasetVerisonEntityType,
  isDifferent: boolean
) => {
  return isDifferent ? styles[`highlightBorder_${entityType}`] : undefined;
};

export const getDiffValueStyle = (
  entityType: DatasetVerisonEntityType,
  isDifferent: boolean
) => {
  return isDifferent
    ? {
        background: getColorByEntityType(entityType),
      }
    : {};
};

export const getColorByEntityType = (entityType: DatasetVerisonEntityType) =>
  entityType === DatasetVerisonEntityType.entity1 ? styles.greenColor : styles.redColor;
