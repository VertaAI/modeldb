import * as Suggestions from 'core/shared/models/HighLevelSearch/Suggestions';

const suggestionsKey: string = 'high-level-search-suggestions';
export const getSuggestionsFromLocalStorage = () => {
  const value = localStorage[suggestionsKey];
  if (!value) {
    return Suggestions.makeSuggestions({});
  } else {
    try {
      const parsedValue = JSON.parse(value);
      return Suggestions.makeSuggestions({
        favorites: parsedValue['favorites'],
        recentSearches: parsedValue['recentSearches'],
      });
    } catch (e) {
      return Suggestions.makeSuggestions({});
    }
  }
};
export const updateSuggestionsToLocalStorage = (
  f: (suggestions: Suggestions.ISuggestions) => Suggestions.ISuggestions
) => {
  localStorage[suggestionsKey] = JSON.stringify(
    f(getSuggestionsFromLocalStorage())
  );
};
