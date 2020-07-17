import * as React from 'react';
import { useDispatch } from 'react-redux';

import * as Suggestions from 'shared/models/HighLevelSearch/Suggestions';

import { getSuggestionsFromLocalStorage } from '../suggestionsFromLocalStorage';
import * as actions from './actions';

const useSuggestionsFromLocalStorage = (initialQuery: string) => {
  const dispatch = useDispatch();
  const [, forceUpdate] = React.useReducer((x) => x + 1, 0);
  const suggestions = getSuggestionsFromLocalStorage();

  React.useEffect(() => {
    actions.addNewRecentSearch(initialQuery);
  }, [initialQuery]);

  return {
    suggestions,
    addFavoriteToSuggestions: (recentSearch: Suggestions.RecentSearch) => {
      dispatch(actions.addFavoriteToSuggestions(recentSearch));
      forceUpdate();
    },
    removeFavoriteFromSuggestions: (favorite: Suggestions.Favorite) => {
      dispatch(actions.removeFavoriteFromSuggestions(favorite));
      forceUpdate();
    },
  };
};

export default useSuggestionsFromLocalStorage;
