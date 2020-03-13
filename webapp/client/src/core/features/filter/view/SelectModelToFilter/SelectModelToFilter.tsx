import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';
import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';

import {
  PropertyType,
  IIdFilterData,
  makeDefaultIdFilter,
} from 'core/features/filter/Model';
import FaiLikeCheckbox from 'core/shared/view/elements/FaiLikeCheckbox/FaiLikeCheckbox';

import {
  addFilterToCurrentContext,
  editFilterInCurrentContext,
  removeFilterFromCurrentContext,
} from '../../store/actions';
import { selectCurrentContextFiltersByType } from '../../store/selectors';
import { IFilterRootState } from '../../store/types';

interface ILocalProps {
  modelId: string;
  onHover?(): void;
  onUnhover?(): void;
}

interface IPropsFromState {
  idFilter: IIdFilterData | undefined;
}

interface IActionProps {
  addFilter: typeof addFilterToCurrentContext;
  editFilter: typeof editFilterInCurrentContext;
  removeFilter: typeof removeFilterFromCurrentContext;
}

type AllProps = ILocalProps & IActionProps & IPropsFromState;

class SelectModelToFilter extends React.PureComponent<AllProps> {
  public render() {
    const { idFilter, modelId, onHover, onUnhover } = this.props;
    return (
      <FaiLikeCheckbox
        value={idFilter ? idFilter.value.includes(modelId) : false}
        iconType="plus"
        labelWhenChecked="remove"
        labelWhenUnchecked="add to collection"
        // todo rename
        dataTest="toggle-filter-by-id"
        onChange={this.onChange}
        onHover={onHover}
        onUnhover={onUnhover}
      />
    );
  }

  @bind
  private onChange(isSelected: boolean) {
    const { addFilter, editFilter, idFilter, modelId } = this.props;

    if (!idFilter) {
      if (isSelected) {
        addFilter(makeDefaultIdFilter(modelId));
        return;
      }
      console.assert('cannot edit filter by id because it is not exist!');
    } else {
      const updatedFilter: IIdFilterData = {
        ...idFilter,
        isEdited: true,
        value: isSelected
          ? idFilter.value.concat(modelId)
          : R.without([modelId], idFilter.value),
      };
      editFilter(updatedFilter);
    }
  }
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators(
    {
      addFilter: addFilterToCurrentContext,
      editFilter: editFilterInCurrentContext,
      removeFilter: removeFilterFromCurrentContext,
    },
    dispatch
  );

const mapStateToProps = (state: IFilterRootState): IPropsFromState => {
  const filters = selectCurrentContextFiltersByType(
    state,
    PropertyType.ID
  )! as IIdFilterData[];
  console.assert(
    [0, 1].includes(filters.length),
    'filters by ids there are more then 1!'
  );

  return {
    idFilter: filters[0],
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SelectModelToFilter);
