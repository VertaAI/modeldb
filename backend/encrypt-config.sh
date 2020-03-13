hash sops >/dev/null 2>&1 || { echo >&2 "Please install sops."; exit 1; }
[ -z "$SOPS_KMS_ARN" ]  && echo "Please set SOPS_KMS_ARN."
sops -e config.k8s.yaml > config.k8s.enc.yaml
rm -f config.k8s.yaml