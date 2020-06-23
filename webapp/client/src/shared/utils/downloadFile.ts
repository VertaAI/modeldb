const downloadFile = ({
  name,
  extension,
  content,
}: {
  name: string;
  extension: string;
  content: string;
}) => {
  const hiddenElement = document.createElement('a');
  hiddenElement.href =
    `data:text/${extension};charset=utf-8,` + encodeURI(content);
  hiddenElement.target = '_blank';
  hiddenElement.download = `${name}.${extension}`;
  hiddenElement.click();
};

export default downloadFile;
