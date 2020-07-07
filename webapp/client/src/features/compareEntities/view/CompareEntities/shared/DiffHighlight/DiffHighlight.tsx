import { EntityType } from 'features/compareEntities/store';

import styles from './DiffHighlight.module.css';

export const getDiffValueBgClassname = (
  entityType: EntityType,
  isDifferent: boolean
) => {
  return isDifferent ? styles[`highlightBg_${entityType}`] : undefined;
};

export const getDiffValueBorderClassname = (
  entityType: EntityType,
  isDifferent: boolean
) => {
  return isDifferent ? styles[`highlightBorder_${entityType}`] : undefined;
};

export const getDiffValueStyle = (
  entityType: EntityType,
  isDifferent: boolean
) => {
  return isDifferent
    ? {
        background: getColorByEntityType(entityType),
      }
    : {};
};

export const getColorByEntityType = (entityType: EntityType) =>
  entityType === EntityType.entity1 ? styles.greenColor : styles.redColor;
