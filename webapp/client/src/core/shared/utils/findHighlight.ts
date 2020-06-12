export type Chunk = { isMatch: boolean; text: string };
// todo rename
const findHighlight = ({
  searchWord,
  textToHighlight,
  settings,
}: {
  searchWord: string;
  textToHighlight: string;
  settings: {
    caseIntensive: boolean;
    allMatches: boolean;
  };
}): Chunk[] => {
  return textToHighlight
    .split(
      new RegExp(
        `(${searchWord})`,
        `${settings.allMatches ? 'g' : ''}${settings.caseIntensive ? 'i' : ''}`
      )
    )
    .filter(Boolean)
    .map(match => ({
      isMatch: searchWord.toLowerCase() === match.toLowerCase(),
      text: match,
    }));
};

export default findHighlight;
