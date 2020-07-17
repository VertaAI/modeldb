import styles from './DiffHighlight.module.css';

const getColor = (entityType: number, type: 'Bg' | 'Border') => {
  return styles[`highlight${type}_${entityType}`];
};

export const getDiffValueBgClassname = (
  entityType: number,
  isDifferent: boolean
) => {
  return isDifferent ? getColor(entityType, 'Bg') : undefined;
};
export const getDiffValueBorderClassname = (
  entityType: number,
  isDifferent: boolean
) => {
  return isDifferent ? getColor(entityType, 'Border') : undefined;
};
