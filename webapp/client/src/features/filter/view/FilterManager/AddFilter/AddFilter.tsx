import React from 'react';

import { IQuickFilter, IFilterData, PropertyType } from 'shared/models/Filters';
import generateId from 'shared/utils/generateId';

import styles from './AddFilter.module.css';

interface ILocalProps {
  quickFilters: IQuickFilter[];
  onCreateFilter: (filter: IFilterData) => void;
}

const AddFilter: React.FC<ILocalProps> = ({ onCreateFilter, quickFilters }) => {
  if (quickFilters.length === 0) {
    return null;
  }

  const newFilter = quickFilters[0];

  const onClick = () => {
    if (newFilter.type === PropertyType.STRING) {
      onCreateFilter({
        type: newFilter.type,
        operator: 'EQUALS',
        value: '',
        isActive: true,
        name: newFilter.propertyName,
        id: generateId(),
      });
    } else {
      onCreateFilter({
        type: newFilter.type,
        operator: 'EQUALS',
        value: undefined,
        isActive: true,
        name: newFilter.propertyName,
        id: generateId(),
      });
    }
  };

  return (
    <div className={styles.root} data-test="add-filter" onClick={onClick}>
      + Add Filter
    </div>
  );
};

export default AddFilter;
