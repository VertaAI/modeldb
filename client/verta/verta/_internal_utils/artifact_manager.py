# -*- coding: utf-8 -*-

class ArtifactResolver(object):
    def get_download_url(self):
        raise NotImplementedError

    def get_upload_url(self, part_num):
        raise NotImplementedError

    def commit_part(self, part_num, tag):
        raise NotImplementedError

    def commit_done(self):
        raise NotImplementedError

# TODO: turn this into an abstract class that other things can inherit/we can provide other types
class ArtifactManager(object):
    def __init__(self, conn, conf, artifact_resolver):
        self._conn = conn
        self._conf = conf
        self._artifact_resolver = artifact_resolver # ideally this becomes an argument so that we have a single manager being used multiple times

    # _upload_artifact
    def upload_stream(self, artifact_stream, part_size=64*(10**6)):
        # TODO: add to Client config
        env_part_size = os.environ.get('VERTA_ARTIFACT_PART_SIZE', "")
        try:
            part_size = int(float(env_part_size))
        except ValueError:  # not an int
            pass
        else:
            print("set artifact part size {} from environment".format(part_size))

        artifact_stream.seek(0)
        if self._conf.debug:
            print("[DEBUG] uploading {} bytes ({})".format(_artifact_utils.get_stream_length(artifact_stream), key))
            artifact_stream.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._artifact_resolver.get_upload_url(part_num=1)

        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: artifact_stream.read(part_size), b'')
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end='\r')

                # get presigned URL
                url = self._artifact_resolver.get_upload_url(part_num=part_num).url

                # wrap file part into bytestream to avoid OverflowError
                #     Passing a bytestring >2 GB (num bytes > max val of int32) directly to
                #     ``requests`` will overwhelm CPython's SSL lib when it tries to sign the
                #     payload. But passing a buffered bytestream instead of the raw bytestring
                #     indicates to ``requests`` that it should perform a streaming upload via
                #     HTTP/1.1 chunked transfer encoding and avoid this issue.
                #     https://github.com/psf/requests/issues/2717
                part_stream = six.BytesIO(file_part)

                # upload part
                #     Retry connection errors, to make large multipart uploads more robust.
                for _ in range(3):
                    try:
                        response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                    except requests.ConnectionError:  # e.g. broken pipe
                        time.sleep(1)
                        continue  # try again
                    else:
                        break
                _utils.raise_for_http_error(response)

                self._artifact_resolver.commit_part(part_num, response.headers['ETag'])
            print()

            # complete upload
            self._artifact_resolver.commit_done()
        else:
            # upload full artifact
            response = _utils.make_request("PUT", url_for_artifact.url, self._conn, data=artifact_stream)
            _utils.raise_for_http_error(response)

    def download_stream(self):
        # download artifact from artifact store
        url = self._artifact_resolver.get_download_url().url

        response = _utils.make_request("GET", url, self._conn)
        _utils.raise_for_http_error(response)
        return response.content

    def download_to_file(self, path):
        # download artifact from artifact store
        url = self._artifact_resolver.get_download_url().url
        with _utils.make_request("GET", url, self._conn, stream=True) as response:
            _utils.raise_for_http_error(response)

            # user-specified filepath, so overwrite
            _request_utils.download(response, path, overwrite_ok=True)
