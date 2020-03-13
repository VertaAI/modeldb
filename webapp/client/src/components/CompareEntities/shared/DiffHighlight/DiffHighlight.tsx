import { EntityType } from 'store/compareEntities';

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
