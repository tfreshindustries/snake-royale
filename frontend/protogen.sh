#!/bin/sh

bin=node_modules/protobufjs/cli/bin
src="../common/src/main/protobuf"
target="./src/proto"

chmod +x "${bin}/pbjs"
chmod +x "${bin}/pbts"

mkdir -p "${target}"

"${bin}/pbjs" -t static-module -w commonjs -o "${target}/model.js" "${src}/*"
"${bin}/pbts" -o "${target}/model.d.ts" "${target}/model.js"
