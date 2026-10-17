#!/usr/bin/env bash
# 用法: ./rag-eval.sh <adminAccessToken> [baseUrl] [evalFile]
# 示例: ./rag-eval.sh eyJhbGci... http://localhost:8084 rag-eval-user.json
set -euo pipefail

TOKEN=${1:?usage: rag-eval.sh <adminAccessToken> [baseUrl] [evalFile]}
BASE=${2:-http://localhost:8084}
FILE=${3:-rag-eval-user.json}

curl -s -X POST "$BASE/api/v1/admin/ai/rag/evaluate" \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "Authorization: Bearer $TOKEN" \
  --data-binary @"$FILE"
echo
