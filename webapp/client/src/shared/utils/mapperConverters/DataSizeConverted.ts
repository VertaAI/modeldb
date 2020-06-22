export const formatBytes = (bytes: string | number, decimals?: number) => {
  if (bytes == 0) return '0 Bytes';
  let k = 1024,
    dm,
    sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
    i = Math.floor(Math.log(Number(bytes)) / Math.log(k));

  if (decimals && decimals !== null && decimals !== undefined) {
    dm = decimals <= 0 ? 0 : decimals || 2;
  } else {
    dm = 2;
  }
  return (
    parseFloat((Number(bytes) / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i]
  );
};
