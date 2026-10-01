#!/usr/bin/env python3
"""Prepare/restore only the installer or official host's unknown-source app-op for acceptance.

This is a pre-granted installation matrix, not evidence of a user's first Settings grant.
No global setting, package default, other app-op or shared UID is modified. The local journal
is written before any device mutation and survives a failed instrumentation process.
Calls targeting the same serial/package must be serialized; the journal is not a process lock.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import uuid

ROOT = Path(__file__).resolve().parent.parent / "build" / "install-permission-journals"
PACKAGES = {"plugin": "io.github.supermonster003.autojs6.plugin.three.setup.installer", "host": "org.autojs.autojs6"}
MODES = {"allow", "ignore", "deny", "default", "foreground"}


def require(value, message):
    if not value:
        raise RuntimeError(message)


def modes(text):
    found = {}
    absent = False
    for line in filter(None, map(str.strip, text.splitlines())):
        if line == "No operations.":
            require(not absent, "Duplicate absence marker")
            absent = True
            continue
        if re.fullmatch(r"Default mode: (allow|ignore|deny|default|foreground)", line):
            require(absent, "Unexpected default-mode annotation")
            continue
        match = re.fullmatch(r"(Uid mode: )?REQUEST_INSTALL_PACKAGES: (allow|ignore|deny|default|foreground)(?:[; ].*)?", line)
        require(match, "Cannot parse installation app-op; no change is safe")
        key = "uid" if match[1] else "package"
        require(key not in found, "Ambiguous installation app-op")
        found[key] = match[2]
    require(found or absent, "Empty installation app-op response")
    require(not (absent and "package" in found), "Conflicting installation app-op response")
    return {"package": found.get("package", "default"), "uid": found.get("uid", "default")}


def verify_restorable(current, original):
    prepared = {"package": "allow", "uid": "default"}
    require(all(current[key] in {original[key], prepared[key]} for key in prepared),
            "Permission changed outside the recorded test; preserve the journal for review")


class Device:
    def __init__(self, adb, serial):
        require(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._:-]{0,127}", serial), "Invalid serial")
        self.adb, self.serial = adb, serial

    def shell(self, *arguments):
        result = subprocess.run([self.adb, "-s", self.serial, "shell", *map(str, arguments)],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30, check=True)
        require(not result.stderr.strip(), "Unexpected device diagnostic: " + result.stderr.decode("utf-8", "replace"))
        return result.stdout.decode("utf-8", "strict").strip()

    def identity(self, package, user):
        if int(self.shell("getprop", "ro.build.version.sdk")) < 26:
            # Older pm has no -U listing. This branch only verifies an already allowed global
            # setting; it never changes an app-op or that device-wide setting.
            dump = self.shell("dumpsys", "package", package)
            require("Package [" + package + "]" in dump and "sharedUser=" not in dump, "Cannot establish the old-platform package identity")
            values = re.findall(r"(?m)^\s*userId=(\d+)\s*$", dump)
            require(len(values) == 1 and int(values[0]) < 100000, "Ambiguous old-platform package UID")
            return int(values[0]) + int(user) * 100000
        text = self.shell("pm", "list", "packages", "-U", "--user", user)
        entries = []
        for line in text.splitlines():
            match = re.fullmatch(r"package:([A-Za-z0-9_.]+) uid:(\d+)", line.strip())
            require(match, "Cannot establish package UID ownership")
            entries.append((match[1], int(match[2])))
        own = [uid for name, uid in entries if name == package]
        require(len(own) == 1, "Target is missing or ambiguous")
        require([name for name, uid in entries if uid == own[0]] == [package], "Refusing a shared UID")
        return own[0]

    def read(self, package, user):
        return modes(self.shell("cmd", "appops", "get", "--user", user, package, "REQUEST_INSTALL_PACKAGES"))

    def set(self, package, user, mode, uid=False):
        require(package in PACKAGES.values() and mode in MODES, "Invalid permission target")
        answer = self.shell("cmd", "appops", "set", "--user", user, *(("--uid",) if uid else ()),
                            package, "REQUEST_INSTALL_PACKAGES", mode)
        require(not answer, "Unexpected permission update response")


def save(path, value):
    temporary = path.with_suffix(".tmp")
    with temporary.open("w", encoding="utf-8", newline="\n") as output:
        json.dump(value, output, indent=2)
        output.flush()
        os.fsync(output.fileno())
    os.replace(temporary, path)


def run(args):
    device = Device(args.adb, args.serial)
    if args.action == "prepare":
        package = PACKAGES[args.target]
        for existing in ROOT.glob("*/journal.json"):
            previous = json.loads(existing.read_text(encoding="utf-8"))
            require(not (previous["serial"] == args.serial and previous["package"] == package and previous["pending"]),
                    "Restore the previous permission journal first: " + str(existing))
        sdk = int(device.shell("getprop", "ro.build.version.sdk"))
        user = int(device.shell("am", "get-current-user"))
        uid = device.identity(package, user)
        data = {"serial": args.serial, "package": package, "sdk": sdk, "user": user, "uid": uid,
                "pending": True, "phase": "preparing"}
        if sdk >= 26:
            data["original"] = device.read(package, user)
        else:
            data["globalUnknownSources"] = device.shell("settings", "get", "secure", "install_non_market_apps")
            require(data["globalUnknownSources"] == "1", "API 24/25 requires an already allowed global setting; this tool does not change it")
        directory = ROOT / uuid.uuid4().hex
        directory.mkdir(parents=True, exist_ok=False)
        path = directory / "journal.json"
        save(path, data)
        if sdk >= 26:
            if data["original"]["uid"] != "default":
                device.set(package, user, "default", uid=True)
            device.set(package, user, "allow")
            require(device.read(package, user) == {"package": "allow", "uid": "default"}, "Preparation was not effective")
        data["phase"] = "prepared"
        save(path, data)
    else:
        path = Path(args.journal).resolve()
        require(path.is_relative_to(ROOT.resolve()) and path.name == "journal.json", "Journal must be owned by this repository tool")
        data = json.loads(path.read_text(encoding="utf-8"))
        require(data["serial"] == args.serial and data["package"] in PACKAGES.values(), "Journal target mismatch")
        require(device.identity(data["package"], data["user"]) == data["uid"], "The original package UID changed")
        require(int(device.shell("getprop", "ro.build.version.sdk")) == data["sdk"], "The device API changed")
        if data["sdk"] >= 26:
            if data["pending"]:
                current = device.read(data["package"], data["user"])
                verify_restorable(current, data["original"])
                if current["package"] != data["original"]["package"]:
                    device.set(data["package"], data["user"], data["original"]["package"])
                # Some API 26-28 CLIs lack --uid. Do not issue a UID write when the original
                # override was untouched; the final read still verifies both exact modes.
                if current["uid"] != data["original"]["uid"]:
                    device.set(data["package"], data["user"], data["original"]["uid"], uid=True)
            require(device.read(data["package"], data["user"]) == data["original"], "Permission modes were not restored")
        else:
            require(device.shell("settings", "get", "secure", "install_non_market_apps") == data["globalUnknownSources"], "Global permission changed during the test")
        data.update(pending=False, phase="restored")
        save(path, data)
    print(json.dumps({"journal": str(path), **data}, indent=2))


def self_test():
    for default in ["default", "deny"]:
        assert modes("No operations.\nDefault mode: " + default) == {"package": "default", "uid": "default"}
    assert modes("Uid mode: REQUEST_INSTALL_PACKAGES: deny\nREQUEST_INSTALL_PACKAGES: allow; time=1") == {"package": "allow", "uid": "deny"}
    verify_restorable({"package": "allow", "uid": "default"}, {"package": "deny", "uid": "deny"})
    verify_restorable({"package": "deny", "uid": "default"}, {"package": "deny", "uid": "deny"})
    try:
        verify_restorable({"package": "ignore", "uid": "default"}, {"package": "default", "uid": "default"})
    except RuntimeError:
        pass
    else:
        raise AssertionError("An unrelated permission change was accepted")
    for invalid in ["", "permission denied", "REQUEST_INSTALL_PACKAGES: invented", "No operations.\nREQUEST_INSTALL_PACKAGES: allow"]:
        try:
            modes(invalid)
        except RuntimeError:
            pass
        else:
            raise AssertionError("Unsafe app-op accepted")
    print("Permission journal self-test passed; no device command executed")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="E:/.android/sdk/platform-tools/adb.exe")
    parser.add_argument("--serial")
    parser.add_argument("--action", choices=["prepare", "restore"])
    parser.add_argument("--target", choices=list(PACKAGES), default="plugin")
    parser.add_argument("--journal")
    parser.add_argument("--self-test", action="store_true")
    options = parser.parse_args()
    if options.self_test:
        self_test()
    else:
        require(options.serial and options.action, "Specify serial and action")
        require(options.action != "restore" or options.journal, "Restore requires the original journal")
        run(options)
