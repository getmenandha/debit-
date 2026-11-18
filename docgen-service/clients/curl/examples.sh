#!/usr/bin/env bash
# curl examples. Run from clients/curl:  BASE=http://localhost:8080 ./examples.sh
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
P="$(dirname "$0")/../payloads"
mkdir -p out

echo "# List document types"
curl -sS "$BASE/api/v1/templates"; echo

echo "# Payload schema and a sample payload for one type"
curl -sS "$BASE/api/v1/templates/TERM_LOAN_AGREEMENT/schema" -o out/term-loan-agreement.schema.json
curl -sS "$BASE/api/v1/templates/TERM_LOAN_AGREEMENT/sample" -o out/term-loan-agreement.sample.json

echo "# PDF bytes (default). -D - prints X-Request-Id, X-Document-Sha256, X-Page-Count, X-Template-Version"
curl -sS -D - -o out/term-loan-agreement.pdf \
  -H "Content-Type: application/json" -H "X-Request-Id: curl-demo-001" \
  --data-binary @"$P/term-loan-agreement.json" \
  "$BASE/api/v1/documents/TERM_LOAN_AGREEMENT?signatureFields=true"

echo "# DOCX via Accept header instead of ?format="
curl -sS -o out/facility-schedule.docx \
  -H "Content-Type: application/json" \
  -H "Accept: application/vnd.openxmlformats-officedocument.wordprocessingml.document" \
  --data-binary @"$P/facility-schedule.json" \
  "$BASE/api/v1/documents/FACILITY_SCHEDULE"

echo "# JSON envelope (base64 + signature anchors) - decode with jq if available"
curl -sS -H "Content-Type: application/json" --data-binary @"$P/loan-summary-es.json" \
  "$BASE/api/v1/documents/LOAN_SUMMARY_ES?format=json" -o out/loan-summary-es.envelope.json
if command -v jq >/dev/null; then
  jq -r .contentBase64 out/loan-summary-es.envelope.json | base64 -d > out/loan-summary-es.pdf
  jq '{requestId, pageCount, sha256, signatureAnchors}' out/loan-summary-es.envelope.json
fi

echo "# Invalid payload -> 400 problem+json listing every error"
curl -sS -w "\nHTTP %{http_code}\n" -H "Content-Type: application/json" \
  --data-binary @"$P/invalid-term-loan-agreement.json" "$BASE/api/v1/documents/TERM_LOAN_AGREEMENT"

ls -l out
