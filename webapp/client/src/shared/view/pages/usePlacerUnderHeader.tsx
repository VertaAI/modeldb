import * as React from 'react';
import * as R from 'ramda';
import matchType from 'shared/utils/matchType';

interface ILocalState {
  height: string;
  horizontalScrollOffset: string;
}

const getHeaderHeight = () => {
  return getComputedStyle(document.documentElement).getPropertyValue(
    '--header-height'
  );
};

const updateHeightOnVerticalScroll = (state: ILocalState) => {
  const offset = parseInt(getHeaderHeight(), 10) - window.scrollY;
  return {
    ...state,
    height: `calc(100% - ${offset < 0 ? 0 : offset}px)`,
  };
};

const updatePosOnHorizontalScroll = (
  position: Position,
  state: ILocalState
): ILocalState => {
  const horizontalScrollOffset = matchType(
    {
      left: () => (window.scrollX > 0 ? `${-window.scrollX}px` : '0'),
      right: () =>
        `${-(
          window.document.body.scrollWidth -
          window.document.body.offsetWidth -
          window.scrollX
        )}px`,
    },
    position
  );
  return { ...state, horizontalScrollOffset };
};

const updateStateOnScroll = (position: Position, state: ILocalState) =>
  R.pipe(
    updateHeightOnVerticalScroll,
    updatedState => updatePosOnHorizontalScroll(position, updatedState)
  )(state);

type Position = 'left' | 'right';

// todo rename
const usePlacerUnderHeader = ({
  position,
}: {
  position: Position;
}): {
  height: React.CSSProperties['height'];
  horizontalScrollOffset: string;
} => {
  const [state, updateStyles] = React.useState<ILocalState>(() => ({
    height: `calc(100% - ${getHeaderHeight()})`,
    horizontalScrollOffset: '0px',
  }));

  const onUpdateStateOnScroll = React.useCallback(() => {
    updateStyles(updateStateOnScroll(position, state));
  }, []);

  React.useEffect(() => {
    onUpdateStateOnScroll();
    window.addEventListener('scroll', onUpdateStateOnScroll);
    return () => window.removeEventListener('scroll', onUpdateStateOnScroll);
  }, []);

  return state;
};

export default usePlacerUnderHeader;
