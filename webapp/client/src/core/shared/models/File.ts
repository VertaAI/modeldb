import * as R from 'ramda';

export const FileExtensions = {
  txt: 'txt',
  text: 'text',
  json: 'json',
  csv: 'csv',
  jpg: 'jpg',
  jpeg: 'jpeg',
  gif: 'gif',
  bmp: 'bmp',
  png: 'png',
} as const;

export type FileExtension = typeof FileExtensions[keyof (typeof FileExtensions)];

export const isFileExtensionImage = (fileExtension: FileExtension): boolean => {
  return [FileExtensions.jpeg, FileExtensions.png, FileExtensions.gif].includes(
    fileExtension as any
  );
};

export const getFileExtension = (str: string): FileExtension | null => {
  const fileType = R.last(str.split('.')) || '';

  return fileType in FileExtensions ? (fileType as FileExtension) : null;
};
