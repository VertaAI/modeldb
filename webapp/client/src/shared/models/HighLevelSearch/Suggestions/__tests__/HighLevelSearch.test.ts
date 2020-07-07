import { addSearch, makeSuggestions, addFavorite } from '../index';

describe('HighLevelSearch', () => {
  describe('addSearch', () => {
    it('should add a new search at the beginning of recent searches', () => {
      expect(
        addSearch('a2', makeSuggestions({ recentSearches: ['a1'] }))
      ).toEqual(
        makeSuggestions({
          recentSearches: ['a2', 'a1'],
        })
      );
    });

    it('should add a search at the beginning of recent searches and delete duplicate if it is', () => {
      expect(
        addSearch('a2', makeSuggestions({ recentSearches: ['a1', 'a2'] }))
      ).toEqual(
        makeSuggestions({
          recentSearches: ['a2', 'a1'],
        })
      );
    });

    it('should add a search at the beginning of recent searches and delete the last search from recent searches if the max size of recent searches is exceeded', () => {
      expect(
        addSearch(
          'a6',
          makeSuggestions({ recentSearches: ['a1', 'a2', 'a3', 'a4', 'a5'] })
        )
      ).toEqual(
        makeSuggestions({
          recentSearches: ['a6', 'a1', 'a2', 'a3', 'a4'],
        })
      );
    });

    it('should not add a search to recent searches if a search is favorite', () => {
      expect(
        addSearch(
          'fa1',
          makeSuggestions({ recentSearches: ['a1'], favorites: ['fa1'] })
        )
      ).toEqual(
        makeSuggestions({
          recentSearches: ['a1'],
          favorites: ['fa1'],
        })
      );
    });
  });

  describe('addFavorite', () => {
    it('should add a recent search at the beggining of favorites and delete one from recent searches', () => {
      expect(
        addFavorite(
          'sd',
          makeSuggestions({ recentSearches: ['sd'], favorites: ['cv'] })
        )
      ).toEqual(
        makeSuggestions({
          recentSearches: [],
          favorites: ['sd', 'cv'],
        })
      );
    });
  });
});
