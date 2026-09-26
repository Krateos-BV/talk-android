#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Krateos BV
# SPDX-License-Identifier: GPL-3.0-or-later
"""Check that app/build.gradle.kts carries a valid, Play-uploadable CalVer version.

Scheme: versionName is YY.M.D.ID and versionCode is the date-encoded integer
YYMMDDID, with ID zero-padded to 3 digits (2026-09-26, ID 0 -> 260926000).
This is files-android's bare formula; there is no offset, because every value
it produces is above RELEASED_BASELINE.

Google Play rejects any upload whose versionCode is not strictly greater than
the last one for the applicationId, so the checks are:

1. versionName and versionCode encode the same date and ID.
2. versionCode is above RELEASED_BASELINE.
3. The code the next calendar day would produce is above the current one, so
   the scheme cannot paint itself into a corner (it would at the 2099 -> 2100
   wrap). The largest code it produces, 991231999, is below Play's
   2100000000 cap.
4. With --base, versionCode did not decrease relative to the base revision.

Usage:
    scripts/xenia/checkVersion.py [--gradle FILE] [--base FILE]
    scripts/xenia/checkVersion.py --self-test
"""
import argparse
import datetime
import re
import sys

# Highest versionCode any artefact of eu.xeniacloud.talk has carried: upstream's
# legacy "25.1.0 Alpha 09", inherited when the applicationId was rebranded.
RELEASED_BASELINE = 250010009

VERSION_CODE_RE = re.compile(r"^\s*versionCode\s*=\s*(\d+)\s*$", re.MULTILINE)
VERSION_NAME_RE = re.compile(r'^\s*versionName\s*=\s*"([^"]*)"\s*$', re.MULTILINE)
CALVER_RE = re.compile(r"^(\d{2})\.([1-9]\d?)\.([1-9]\d?)\.(0|[1-9]\d{0,2})$")


def encode(date, build_id):
    return (date.year % 100) * 10_000_000 + date.month * 100_000 + date.day * 1_000 + build_id


def read_version(text):
    """Return (versionCode, versionName) of defaultConfig, the first pair in the file."""
    code = VERSION_CODE_RE.search(text)
    name = VERSION_NAME_RE.search(text)
    if not code or not name:
        raise ValueError("versionCode/versionName not found")
    return int(code.group(1)), name.group(1)


def check(text, base_text=None):
    """Return a list of human-readable failures; empty means the version is valid."""
    try:
        code, name = read_version(text)
    except ValueError as e:
        return [str(e)]

    match = CALVER_RE.match(name)
    if not match:
        return [f'versionName "{name}" is not YY.M.D.ID (no leading zeros, ID 0-999)']
    yy, month, day, build_id = (int(g) for g in match.groups())
    try:
        date = datetime.date(2000 + yy, month, day)
    except ValueError:
        return [f'versionName "{name}" is not a real calendar date']

    errors = []
    expected = encode(date, build_id)
    if code != expected:
        errors.append(f'versionCode {code} does not encode versionName "{name}" (expected {expected})')
    if code <= RELEASED_BASELINE:
        errors.append(f"versionCode {code} is not above the released baseline {RELEASED_BASELINE}")
    successor = encode(date + datetime.timedelta(days=1), 0)
    if successor <= code:
        errors.append(f"the next day's versionCode {successor} would not be above {code}")

    if base_text is not None:
        try:
            base_code, _ = read_version(base_text)
        except ValueError:
            base_code = None
        if base_code is not None and code < base_code:
            errors.append(f"versionCode {code} is lower than the base revision's {base_code}")
    return errors


def gradle(code, name):
    return f'android {{\n    defaultConfig {{\n        versionCode = {code}\n        versionName = "{name}"\n    }}\n}}\n'


def self_test():
    """Prove every check can fail, so a green run means something."""
    cases = [
        ("valid", gradle(260926000, "26.9.26.0"), None, True),
        ("valid with build id", gradle(261231012, "26.12.31.12"), None, True),
        ("valid, base lower", gradle(261001000, "26.10.1.0"), gradle(260926000, "26.9.26.0"), True),
        ("upstream legacy name", gradle(250010009, "25.1.0 Alpha 09"), None, False),
        ("name/code mismatch", gradle(260926001, "26.9.26.0"), None, False),
        ("notes-android offset", gradle(660926000, "26.9.26.0"), None, False),
        ("leading zero month", gradle(260926000, "26.09.26.0"), None, False),
        ("impossible date", gradle(260230000, "26.2.30.0"), None, False),
        ("century wrap", gradle(991231000, "99.12.31.0"), None, False),
        ("below baseline", gradle(241231000, "24.12.31.0"), None, False),
        ("decreased vs base", gradle(260926000, "26.9.26.0"), gradle(261001000, "26.10.1.0"), False),
        ("missing fields", "android {}\n", None, False),
    ]
    failed = 0
    for label, text, base, want_ok in cases:
        errors = check(text, base)
        if (not errors) != want_ok:
            failed += 1
            print(f"SELF-TEST FAIL: {label}: expected {'pass' if want_ok else 'failure'}, got {errors or 'pass'}")
        else:
            print(f"ok: {label}{': ' + errors[0] if errors else ''}")
    return failed == 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--gradle", default="app/build.gradle.kts", help="build script to check")
    parser.add_argument("--base", help="the same build script at the base revision")
    parser.add_argument("--self-test", action="store_true", help="run the built-in regression cases")
    args = parser.parse_args()

    if args.self_test:
        return 0 if self_test() else 1

    with open(args.gradle, encoding="utf-8") as f:
        text = f.read()
    base_text = None
    if args.base:
        with open(args.base, encoding="utf-8") as f:
            base_text = f.read()

    errors = check(text, base_text)
    for error in errors:
        print(f"::error file={args.gradle}::{error}")
    if not errors:
        code, name = read_version(text)
        print(f'OK: versionCode {code}, versionName "{name}"')
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
