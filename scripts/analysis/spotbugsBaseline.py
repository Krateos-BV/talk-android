#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Krateos BV
# SPDX-License-Identifier: GPL-3.0-or-later
"""Write the committed Spotbugs baseline from a gplayDebug report.

The baseline keeps only what spotbugsSummary.py counts: each category's name and one
BugInstance per finding, sorted, so the file is small and identical for identical counts.

Usage: ./gradlew spotbugsGplayDebug && scripts/analysis/spotbugsBaseline.py
"""
import argparse
from xml.sax.saxutils import escape, quoteattr

import defusedxml.ElementTree as ET

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", help="Spotbugs report to read", default="app/build/reports/spotbugs/gplayDebug.xml")
    parser.add_argument("--output", help="baseline to write", default="scripts/analysis/spotbugs-baseline.xml")
    args = parser.parse_args()

    root = ET.parse(args.file).getroot()
    counts = {}
    names = {}
    for child in root:
        if child.tag == "BugInstance":
            category = child.attrib["category"]
            counts[category] = counts.get(category, 0) + 1
        elif child.tag == "BugCategory":
            names[child.attrib["category"]] = child[0].text

    lines = ["<?xml version='1.0' encoding='UTF-8'?>", "<BugCollection>"]
    for category in sorted(counts):
        lines.append(f"  <BugCategory category={quoteattr(category)}>"
                     f"<Description>{escape(names[category])}</Description></BugCategory>")
    for category in sorted(counts):
        lines += [f"  <BugInstance category={quoteattr(category)} />"] * counts[category]
    lines.append("</BugCollection>")

    with open(args.output, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
