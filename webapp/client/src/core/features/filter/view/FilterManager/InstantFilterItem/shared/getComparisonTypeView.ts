import { ComparisonType } from 'core/features/filter/Model';

const comparisonTypeView: Record<ComparisonType, string> = {
  [ComparisonType.EQUALS]: '=',
  [ComparisonType.LESS]: '<',
  [ComparisonType.LESS_OR_EQUALS]: '<=',
  [ComparisonType.MORE]: '>',
  [ComparisonType.GREATER_OR_EQUALS]: '>=',
};

const getComparisonTypeView = (comparisonType: ComparisonType): string => {
  return comparisonTypeView[comparisonType];
};

export default getComparisonTypeView;
