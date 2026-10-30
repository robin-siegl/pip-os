#!/usr/bin/env python3
"""Capture the real PIP-OS UI from an Android emulator using ADB/UIAutomator.

Requires: running emulator, installed dev.pipos.launcher debug APK.
Captures ordinary display output including system bars. No test runner required.
"""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUTPUT = Path("docs/screenshots")
OUTPUT.mkdir(parents=True, exist_ok=True)


def adb(*args, timeout=35):
    return subprocess.run(["adb", *args], check=True, capture_output=True, timeout=timeout).stdout


def dump_nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/pipos-layout.xml", timeout=50)
    root = ET.fromstring(adb("shell", "cat", "/sdcard/pipos-layout.xml"))
    return list(root.iter("node"))


def tap_text(text, optional=False):
    for attempt in range(12):
        try:
            nodes = dump_nodes()
            candidates = [
                node for node in nodes
                if node.attrib.get("text") == text
                or node.attrib.get("content-desc") == text
            ]
            if candidates:
                bounds = candidates[0].attrib["bounds"]
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", bounds))
                adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                time.sleep(1.2)
                return True
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, ET.ParseError):
            pass
        time.sleep(1.5)
    if optional:
        return False
    raise RuntimeError(f"Could not find on-screen label: {text!r}")


def screenshot(filename):
    time.sleep(1.8)
    data = adb("exec-out", "screencap", "-p")
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError(f"Screenshot was not PNG data: {filename}")
    (OUTPUT / filename).write_bytes(data)
    print(f"Saved {OUTPUT / filename}: {len(data)} bytes", flush=True)


print("Launching PIP-OS", flush=True)
adb("shell", "am", "start", "-n", "dev.pipos.launcher/.MainActivity")
time.sleep(3)
tap_text("NOT NOW", optional=True)
screenshot("apps.png")

tap_text("EDIT")
screenshot("apps-edit.png")
tap_text("DONE")

tap_text("STAT")
screenshot("stat.png")

tap_text("CFG")
screenshot("settings.png")
print("All four screenshots captured successfully.", flush=True)
