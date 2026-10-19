"""Exercise the publishing shell without a Git checkout or GitHub credentials."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import textwrap
import unittest


WORKFLOW = Path(__file__).resolve().parents[1] / ".github/workflows/release.yml"
STEP = "      - name: Create or resume a draft, then upload all files\n"

GH_STUB = '''
import json
import os
from pathlib import Path
import sys

args = sys.argv[1:]
with open(os.environ["GH_TEST_CALLS"], "a") as calls:
    calls.write(json.dumps(args) + "\\n")
scenario = os.environ["GH_TEST_SCENARIO"]
if args[:2] == ["release", "view"]:
    if scenario == "draft": print("true")
    elif scenario == "published": print("false")
    else: sys.exit(1)
elif args[:1] == ["api"]:
    if scenario == "api-error": sys.exit(37)
    if "/git/ref/tags/" in args[1]:
        print("" if scenario == "lightweight" else "a" * 40)
    elif "/git/tags/" in args[1]:
        if scenario == "empty-notes": print("")
        elif scenario == "whitespace-notes": print(" \\t\\n")
        else: print("Elternübersicht: Beobachtungen bleiben sichtbar.")
    else: sys.exit("Unexpected API request")
elif args[:2] == ["release", "create"]:
    assert "--notes-from-tag" not in args
    assert "--generate-notes" not in args
    assert "--draft" in args and "--verify-tag" in args
    notes = Path(args[args.index("--notes-file") + 1]).read_text()
    assert notes == "Elternübersicht: Beobachtungen bleiben sichtbar.\\n"
elif args[:2] != ["release", "upload"]:
    sys.exit("Unexpected gh invocation")
'''


@unittest.skipUnless(os.name != "nt" and shutil.which("bash"), "Publishing runs in Bash on Ubuntu")
class ReleaseWorkflowTest(unittest.TestCase):
    def run_publish(self, scenario):
        step = WORKFLOW.read_text().split(STEP, 1)[1].split("\n      - name:", 1)[0]
        shell = textwrap.dedent(step.split("        run: |\n", 1)[1])
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            stub = root / "gh"
            stub.write_text("#!" + sys.executable + "\n" + textwrap.dedent(GH_STUB))
            stub.chmod(0o755)
            (root / "dist").mkdir()
            (root / "dist/mod.jar").write_bytes(b"tested artifact")
            calls_path = root / "calls.jsonl"
            environment = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                               GH_TEST_CALLS=str(calls_path), GH_TEST_SCENARIO=scenario,
                               RUNNER_TEMP=str(root), GH_REPO="owner/repository", RELEASE_TAG="v0.3.4")
            result = subprocess.run(["bash", "-c", shell], cwd=root, env=environment,
                                    capture_output=True, text=True, timeout=20)
            calls = [json.loads(line) for line in calls_path.read_text().splitlines()]
        return result, calls

    def test_new_release_reads_annotation_remotely_without_checkout(self):
        result, calls = self.run_publish("new")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(["release", "create"], calls[-2][:2])
        self.assertEqual(["release", "upload"], calls[-1][:2])
        self.assertIn("repos/owner/repository/git/ref/tags/v0.3.4", calls[1])
        self.assertIn("repos/owner/repository/git/tags/" + "a" * 40, calls[2])

    def test_existing_draft_can_resume_upload(self):
        result, calls = self.run_publish("draft")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual([["release", "view"], ["release", "upload"]], [call[:2] for call in calls])

    def test_published_release_is_never_modified(self):
        result, calls = self.run_publish("published")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(1, len(calls))

    def test_missing_annotation_or_api_errors_prevent_creation_and_upload(self):
        for scenario in ("api-error", "lightweight", "empty-notes", "whitespace-notes"):
            with self.subTest(scenario=scenario):
                result, calls = self.run_publish(scenario)
                self.assertNotEqual(0, result.returncode)
                self.assertFalse(any(call[:2] in (["release", "create"], ["release", "upload"])
                                     for call in calls))


if __name__ == "__main__":
    unittest.main()
