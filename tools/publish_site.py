# SPDX-License-Identifier: GPL-3.0-or-later
"""Publishes site/ to the public repository that GitHub Pages serves (mrghxst/mensa).

Usage (from the repository root):
    python tools/publish_site.py            # show what would change
    python tools/publish_site.py --push     # commit and push it

The Pages repository holds only the contents of site/. This script clones it into a temporary
folder, replaces its files with site/, and commits with the current commit of this repository
in the message. It needs git and push access to the Pages repository.
"""
import argparse
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SITE = ROOT / "site"
REMOTE = "https://github.com/mrghxst/mensa.git"
AUTHOR = ["-c", "user.name=mrghxst", "-c", "user.email=67842132+mrghxst@users.noreply.github.com"]


def git(*args: str, cwd: Path) -> str:
    return subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True).stdout.strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--push", action="store_true", help="commit and push instead of only showing the changes")
    args = parser.parse_args()

    source = git("rev-parse", "--short", "HEAD", cwd=ROOT)
    dirty = git("status", "--porcelain", "--", "site", cwd=ROOT)
    with tempfile.TemporaryDirectory(prefix="mensa-site-") as tmp:
        work = Path(tmp)
        subprocess.run(["git", "clone", "--quiet", "--depth", "1", REMOTE, str(work)], check=True)
        for entry in work.iterdir():
            if entry.name == ".git":
                continue
            shutil.rmtree(entry) if entry.is_dir() else entry.unlink()
        shutil.copytree(SITE, work, dirs_exist_ok=True)
        git("add", "--all", cwd=work)
        changes = git("status", "--short", cwd=work)
        if not changes:
            print("The published site is already up to date.")
            return 0
        print(changes)
        if not args.push:
            print("\nRun with --push to publish these changes.")
            return 0
        note = " (with uncommitted changes)" if dirty else ""
        git(*AUTHOR, "commit", "--quiet", "-m", f"Update the site from mensa-zurich {source}{note}", cwd=work)
        git("push", "--quiet", "origin", "HEAD:main", cwd=work)
        print("\nPublished. GitHub Pages updates within a minute or two.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
