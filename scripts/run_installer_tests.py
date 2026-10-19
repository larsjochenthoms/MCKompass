"""Run native installer tests and expose failures in the GitHub check report."""
import io
import os
from pathlib import Path
import sys
import unittest


def main():
    scripts = Path(__file__).resolve().parent
    suite = unittest.defaultTestLoader.discover(str(scripts / "tests"), pattern="test_installers.py")
    output = io.StringIO()
    result = unittest.TextTestRunner(stream=output, verbosity=2).run(suite)
    report = output.getvalue()
    print(report, end="", flush=True)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as stream:
            stream.write("## Native installer tests\n\n```text\n" + report + "\n```\n")
    if os.environ.get("GITHUB_ACTIONS") == "true":
        for test, traceback in result.failures + result.errors:
            message = (str(test) + "\n" + traceback).replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
            print("::error file=scripts/tests/test_installers.py,title=Installer test failed::" + message, flush=True)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main())
