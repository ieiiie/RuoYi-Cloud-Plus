#!/usr/bin/env bash
# Builds an image only. It never creates, starts, stops or replaces any container.
set -euo pipefail
module_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
: "${JAVA_RUNTIME_IMAGE:?Set an approved Java 21 runtime image with @sha256 digest}"
if [[ ! "$JAVA_RUNTIME_IMAGE" =~ @sha256:[a-f0-9]{64}$ ]]; then
  echo 'JAVA_RUNTIME_IMAGE must use an immutable sha256 digest' >&2
  exit 2
fi
jar_file="$module_dir/target/ym-agriculture.jar"
[[ -f "$jar_file" ]] || { echo 'Build target/ym-agriculture.jar first' >&2; exit 2; }
jar_sha=$(sha256sum "$jar_file" | cut -d ' ' -f 1)
image_tag=${IMAGE_TAG:-ym-agriculture:jetlinks-${jar_sha:0:12}}
docker build --pull=false --network=none --file "$module_dir/Dockerfile.jetlinks" \
  --build-arg "JAVA_RUNTIME_IMAGE=$JAVA_RUNTIME_IMAGE" --build-arg "JAR_SHA256=$jar_sha" \
  --tag "$image_tag" "$module_dir"
echo "Built $image_tag from jar sha256:$jar_sha"
