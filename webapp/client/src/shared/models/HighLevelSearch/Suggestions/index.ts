import * as R from 'ramda';

export type RecentSearch = string;
export type Favorite = RecentSearch;
export interface ISuggestions {
  recentSearches: RecentSearch[];
  favorites: Favorite[];
}
const recentSearchesSize = 5;

export const makeSuggestions = ({
  recentSearches = [],
  favorites = [],
}: Partial<ISuggestions>): ISuggestions => {
  const emptySuggestions: ISuggestions = {
    favorites: [],
    recentSearches: [],
  };
  return R.pipe(
    (res: ISuggestions) =>
      R.reverse(recentSearches).reduce(
        (res, search) => addSearch(search, res),
        res
      ),
    (res: ISuggestions) =>
      R.reverse(favorites).reduce(
        (res, favorite) => addFavorite(favorite, res),
        res
      )
  )(emptySuggestions);
};

export const addSearch = (
  value: string,
  suggestions: ISuggestions
): ISuggestions => {
  return {
    ...suggestions,
    recentSearches:
      suggestions.favorites.includes(value) || !value
        ? suggestions.recentSearches
        : R.pipe(
            R.prepend(value),
            R.uniq,
            recentSearches =>
              recentSearches.length > recentSearchesSize
                ? R.init(recentSearches)
                : recentSearches
          )(suggestions.recentSearches),
  };
};

export const addFavorite = (
  recentSearch: RecentSearch,
  suggestions: ISuggestions
): ISuggestions => {
  return {
    ...suggestions,
    recentSearches: R.without([recentSearch], suggestions.recentSearches),
    favorites: R.prepend(recentSearch, suggestions.favorites),
  };
};

export const removeFavorite = (
  favorite: Favorite,
  suggestions: ISuggestions
): ISuggestions => {
  return {
    ...suggestions,
    favorites: R.without([favorite], suggestions.favorites),
  };
};
