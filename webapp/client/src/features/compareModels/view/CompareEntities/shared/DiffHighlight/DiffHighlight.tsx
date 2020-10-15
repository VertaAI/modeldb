import styles from './DiffHighlight.module.css';

const colorsAmount = 6;

const getColor = (entityType: number, type: 'Bg' | 'Border') => {
  const colorNum = (entityType % colorsAmount) + 1;
  return styles[`highlight${type}_${colorNum}`];
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
