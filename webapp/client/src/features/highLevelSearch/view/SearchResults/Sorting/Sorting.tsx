import * as React from 'react';
import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';

import Select, {
  IOptionType,
} from 'shared/view/elements/Selects/Select/Select';
import { actions } from 'features/highLevelSearch/store';
import { IResultsSorting } from 'shared/models/HighLevelSearch';
import { IconType } from 'shared/view/elements/Icon/Icon';

interface ILocalProps {
  value: IResultsSorting | undefined;
}

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      changeSorting: actions.changeSearchSettingsSorting,
    },
    dispatch
  );
};

type AllProps = ILocalProps & ReturnType<typeof mapDispatchToProps>;

const Sorting = ({ value, changeSorting }: AllProps) => {
  const getDirectionIcon = (
    direction: IResultsSorting['direction']
  ): IconType => (direction === 'asc' ? 'sort-ascending' : 'sort-descending');
  const fieldOptions: Array<IOptionType<'dateCreated' | 'dateUpdated'>> = [
    {
      label: 'Date Created',
      value: 'dateCreated',
      iconType:
        value && value.field === 'dateCreated'
          ? getDirectionIcon(value.direction)
          : undefined,
    },
    {
      label: 'Date Updated',
      value: 'dateUpdated',
      iconType:
        value && value.field === 'dateUpdated'
          ? getDirectionIcon(value.direction)
          : undefined,
    },
  ];

  return (
    <div>
      <Select
        inputText="Sort by"
        options={fieldOptions}
        value={fieldOptions.find(
          option => option.value === (value && value.field)
        )}
        customStyles={{
          menu: { width: '153px', position: 'right' },
        }}
        onChange={newOptionField => {
          if (!value || newOptionField.value !== value.field) {
            changeSorting({
              direction: 'desc',
              field: newOptionField.value,
            });
            return;
          }
          if (value.direction === 'desc') {
            changeSorting({
              direction: 'asc',
              field: newOptionField.value,
            });
            return;
          }
          if (value.direction === 'asc') {
            changeSorting(undefined);
          }
        }}
      />
    </div>
  );
};

export default connect(
  undefined,
  mapDispatchToProps
)(Sorting);
