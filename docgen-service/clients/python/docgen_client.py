#!/usr/bin/env python3
"""Sample consumer of the document generation API. Python 3.8+, standard library only.

    python3 docgen_client.py                                  # demos against http://localhost:8080
    python3 docgen_client.py https://docgen.internal.example  # another base URL
"""
import base64
import json
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path


class DocGenError(Exception):
    def __init__(self, status, problem):
        super().__init__(f"HTTP {status}: {problem}")
        self.status = status
        self.problem = problem  # RFC 9457 problem+json; 400s carry an "errors" list


class DocGenClient:
    def __init__(self, base_url="http://localhost:8080", timeout=60):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def _call(self, method, path, body=None, headers=None):
        req = urllib.request.Request(self.base_url + path, data=body, method=method, headers=headers or {})
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as res:
                return res.read(), dict(res.headers)
        except urllib.error.HTTPError as e:
            raw = e.read()
            try:
                problem = json.loads(raw)
            except ValueError:
                problem = raw.decode(errors="replace")
            raise DocGenError(e.code, problem) from None

    def list_templates(self):
        return json.loads(self._call("GET", "/api/v1/templates")[0])

    def sample_payload(self, document_type):
        return json.loads(self._call("GET", f"/api/v1/templates/{document_type}/sample")[0])

    def generate(self, document_type, payload, fmt="pdf", signature_fields=False, request_id=None):
        """Returns (document_bytes, headers). fmt: pdf | docx."""
        headers = {"Content-Type": "application/json", "X-Request-Id": request_id or str(uuid.uuid4())}
        path = f"/api/v1/documents/{document_type}?format={fmt}&signatureFields={str(signature_fields).lower()}"
        return self._call("POST", path, json.dumps(payload).encode(), headers)

    def generate_envelope(self, document_type, payload):
        """Returns the JSON envelope: contentBase64, pageCount, sha256, signatureAnchors, warnings, ..."""
        body, _ = self._call("POST", f"/api/v1/documents/{document_type}?format=json",
                             json.dumps(payload).encode(), {"Content-Type": "application/json"})
        return json.loads(body)


def main():
    client = DocGenClient(sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080")
    payloads = Path(__file__).resolve().parent.parent / "payloads"
    out = Path("out")
    out.mkdir(exist_ok=True)
    load = lambda name: json.loads((payloads / name).read_text(encoding="utf-8"))

    print("1) Document types:", [t["documentType"] for t in client.list_templates()])

    print("2) Term loan agreement -> PDF bytes")
    pdf, h = client.generate("TERM_LOAN_AGREEMENT", load("term-loan-agreement.json"), signature_fields=True)
    (out / "term-loan-agreement.pdf").write_bytes(pdf)
    print(f"   {out/'term-loan-agreement.pdf'}  {len(pdf):,} bytes, {h.get('X-Page-Count')} pages, "
          f"request {h.get('X-Request-Id')}")

    print("3) W-9 style taxpayer form -> PDF, payload pulled from the service's own sample endpoint")
    pdf, _ = client.generate("TAXPAYER_FORM", client.sample_payload("TAXPAYER_FORM"))
    (out / "taxpayer-form.pdf").write_bytes(pdf)
    print(f"   {out/'taxpayer-form.pdf'}  {len(pdf):,} bytes")

    print("4) Spanish loan summary -> JSON envelope")
    env = client.generate_envelope("LOAN_SUMMARY_ES", load("loan-summary-es.json"))
    (out / "loan-summary-es.pdf").write_bytes(base64.b64decode(env["contentBase64"]))
    print(f"   {out/'loan-summary-es.pdf'}  {env['pageCount']} pages, sha256 {env['sha256'][:16]}...")

    print("5) Invalid payload -> 400")
    try:
        client.generate("TERM_LOAN_AGREEMENT", load("invalid-term-loan-agreement.json"))
    except DocGenError as e:
        print(f"   HTTP {e.status}")
        for err in e.problem.get("errors", []):
            print(f"     {err['path']}: {err['message']}")


if __name__ == "__main__":
    main()
