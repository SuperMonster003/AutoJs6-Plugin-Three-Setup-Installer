#!/usr/bin/env python3
"""Guarded P6 process-death probes. No root shell, package removal, or global process stop.

The plugin-main mode kills exactly the recorded plugin main PID after a real privileged
write. The host mode kills only AutoJs6's dedicated debug caller process while a different
private host process keeps its read-only source pipe open. Existing install sessions are
read-only baselines; only the newly returned platform session ID is inspected.

Examples (run from the plugin repository):
  py tools/run-process-death-acceptance.py --serial SERIAL --mode plugin-main --authorizer root
  py tools/run-process-death-acceptance.py --serial SERIAL --mode host --authorizer none --fixture build/performance-fixture/fixture.apk
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import uuid

PLUGIN = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
HOST = "org.autojs.autojs6"
PLUGIN_COMPONENT = PLUGIN + "/.spike.InstallProcessDeathProbeActivity"
HOST_COMPONENT = HOST + "/org.autojs.autojs.core.plugin.installer.InstallerProcessProbeActivity"
SPIKE = "io.github.supermonster003.autojs6.installer.spike.fixture"
PERFORMANCE = "io.github.supermonster003.autojs6.installer.performance.fixture"
REPO = Path(__file__).resolve().parent.parent


def require(value, message):
    if not value:
        raise RuntimeError(message)
    return value


def active_sessions(dump: str) -> tuple[str, dict[int, str]]:
    match = re.search(r"(?m)^Active install sessions:\s*\n", dump)
    require(match, "Cannot identify the platform's active-install-session section")
    tail = dump[match.end():]
    boundary = re.search(r"(?m)^(?:Finalized|Historical) install sessions:", tail)
    require(boundary, "Cannot distinguish active sessions from historical sessions")
    body = tail[:boundary.start()]
    entries = list(re.finditer(r"(?m)^\s*(?:Active )?(?:Child )?Session (\d+):\s*\n", body))
    sessions = {int(item.group(1)): body[item.start():entries[i + 1].start() if i + 1 < len(entries) else len(body)]
                for i, item in enumerate(entries)}
    require(len(sessions) == len(entries), "Duplicate active platform session IDs")
    require(not body.strip() or entries, "Unrecognized nonempty active session list")
    return "Active install sessions:\n" + body, sessions


class Device:
    def __init__(self, adb: str, serial: str):
        self.adb, self.serial = adb, serial

    def run(self, *args, data=None, check=True, timeout=60) -> bytes:
        result = subprocess.run([self.adb, "-s", self.serial, *map(str, args)], input=data,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if check and result.returncode:
            raise RuntimeError(f"adb {' '.join(map(str, args[:5]))}: {result.stderr.decode('utf-8', 'replace').strip()}")
        return result.stdout

    def shell(self, *args, **kwargs):
        return self.run("shell", *args, **kwargs).decode("utf-8", "replace").replace("\r\n", "\n")

    def private(self, package, path):
        value = self.run("exec-out", "run-as", package, "cat", path, check=False)
        # Some Android exec-out failures incorrectly return exit code zero; data is validated.
        return value

    def document(self, package, path):
        try:
            value = json.loads(self.private(package, path))
            return value if isinstance(value, dict) else None
        except (ValueError, UnicodeError):
            return None

    def create_case(self, package, path, source: Path | None):
        require(re.fullmatch(r"files/p6-(?:process-death|installer-probe)/[a-f0-9]{32}", path), "Unsafe private case path")
        # The leaf must not preexist. Shell text contains only generated, validated path segments.
        result = self.shell("run-as", package, "sh", "-c", f"'mkdir -p {path.rsplit('/', 1)[0]} && mkdir {path}'")
        require(not result.strip(), f"Cannot create a new private case: {result}")
        if source is not None:
            remote = "/data/local/tmp/p6-process-source-" + path.rsplit("/", 1)[1] + ".apk"
            owns_transfer = False
            try:
                require(self.shell("sh", "-c", f"'test ! -e {remote} && echo absent'").strip() == "absent", "Remote transfer path already exists")
                owns_transfer = True
                # Windows adb stdin uses text mode and can stop at byte 0x1a. The sync protocol
                # preserves binary APK data; run-as copies only this generated, owned temp file.
                self.run("push", str(source.resolve()), remote)
                self.shell("run-as", package, "cp", remote, path + "/fixture.apk")
                self.shell("run-as", package, "chmod", "400", path + "/fixture.apk")
                digest = self.shell("run-as", package, "sha256sum", path + "/fixture.apk").split()[0]
                require(digest == hashlib.sha256(source.read_bytes()).hexdigest(), "Private fixture transfer hash differs")
            except Exception:
                self.shell("run-as", package, "rm", "-f", path + "/fixture.apk")
                self.shell("run-as", package, "rmdir", path)
                raise
            finally:
                if owns_transfer:
                    self.shell("rm", "-f", remote)

    def launch(self, component, case_id, mode, authorizer, expected_pid=None):
        args = ["am", "start", *([] if mode == "die" else ["-W"]), "-f", "0x18000000", "-n", component,
                "--es", "caseId", case_id, "--es", "mode", mode, "--es", "authorizer", authorizer]
        if expected_pid is not None:
            args += ["--ei", "expectedPid", str(expected_pid)]
        output = self.shell(*args)
        require("Error:" not in output and "Exception" not in output and
                ((mode == "die" and "Starting: Intent" in output) or "Status: ok" in output),
                f"Probe Activity did not start: {output}")

    def wait_enable_journal(self, directory, phase, seconds=20):
        deadline = time.monotonic() + seconds
        last = None
        while time.monotonic() < deadline:
            last = self.document(HOST, directory + "/enable-restore.json")
            if last and last.get("phase") == phase:
                require(last.get("targetPackage") == PLUGIN and last.get("caseId") == directory.rsplit("/", 1)[1], "Enable journal identity differs")
                fingerprints = last.get("fingerprints", [])
                require(fingerprints and fingerprints == sorted(set(fingerprints)) and all(re.fullmatch(r"[0-9a-f]{64}", x) for x in fingerprints), "Invalid signer evidence")
                return last
            time.sleep(0.2)
        raise RuntimeError(f"Enable preference did not reach {phase}: {last}; error={self.document(HOST, directory + '/error.json')}")

    def wait_document(self, package, directory, phase, seconds=45, ignore_error=False):
        deadline = time.monotonic() + seconds
        last = None
        while time.monotonic() < deadline:
            if not ignore_error:
                error = self.document(package, directory + "/error.json")
                require(error is None, f"Probe rejected or failed: {error}")
            last = self.document(package, directory + "/evidence.json")
            if last and last.get("phase") == phase:
                return last
            if not ignore_error:
                require(not last or last.get("phase") not in {"failed", "cancelled-before-main-death", "ended-before-host-death"},
                        f"Probe ended before the guarded death point: {last}")
            time.sleep(0.2)
        raise RuntimeError(f"No {phase} evidence appeared: {last}")

    def identity(self, package, pid, expected_name):
        require(isinstance(pid, int) and pid > 1, "Invalid owned process ID")
        prefix = f"/proc/{pid}/"
        name = self.private(package, prefix + "cmdline").rstrip(b"\0").decode("utf-8", "replace")
        require(name == expected_name, f"Refuse to signal unexpected process {pid}: {name}")
        status = self.private(package, prefix + "status").decode("utf-8", "replace")
        found = re.search(r"(?m)^Uid:\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)", status)
        require(found, "Cannot establish the owned process UID")
        uids = set(map(int, found.groups()))
        require(len(uids) == 1, "Unexpected process credential transition")
        stat = self.private(package, prefix + "stat").decode("utf-8", "replace")
        require(") " in stat, "Cannot establish process start time")
        fields = stat.rsplit(") ", 1)[1].split()
        require(len(fields) > 19 and fields[19].isdigit(), "Invalid process start time")
        return {"pid": pid, "uid": uids.pop(), "name": name, "startTicks": int(fields[19])}

    def kill_owned(self, package, identity, component, case_id, authorizer):
        require(self.identity(package, identity["pid"], identity["name"]) == identity, "The process changed before kill")
        # Signal 0 checks permission without delivering a signal. Sony's toybox calls EACCES
        # "unknown pid"; the shell builtin reports the actual denial and never accepts names.
        signal_check = self.shell("run-as", package, "sh", "-c", f"'kill -0 {identity['pid']} 2>&1'", check=False).strip()
        if signal_check:
            require("Permission denied" in signal_check, f"Unexpected signal permission failure: {signal_check}")
            require(self.identity(package, identity["pid"], identity["name"]) == identity, "The process changed before guarded self-death")
            mechanism = {"method": "debug-self-death", "reason": "run-as signal EACCES", "signalPermissionEvidence": signal_check}
            self.launch(component, case_id, "die", authorizer, expected_pid=identity["pid"])
        else:
            mechanism = {"method": "run-as SIGKILL"}
            self.shell("run-as", package, "kill", "-9", identity["pid"])
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            if self.private(package, f"/proc/{identity['pid']}/cmdline").rstrip(b"\0") != identity["name"].encode():
                return mechanism
            time.sleep(0.1)
        raise RuntimeError("The exact owned PID did not exit")

    def fixture_absent(self, fixture):
        users = re.findall(r"UserInfo\{(\d+):", self.shell("pm", "list", "users"))
        require(users, "Cannot enumerate device users")
        for user in users:
            lines = [line.strip() for line in self.shell("pm", "list", "packages", "-u", "--user", user).splitlines() if line.strip()]
            require("package:android" in lines and all(line.startswith("package:") for line in lines), "Cannot safely enumerate user packages")
            require("package:" + fixture not in lines, f"Fixture or retained fixture data already exists for user {user}")

    def sessions(self):
        return active_sessions(self.shell("dumpsys", "package", timeout=90))

    def remove_case(self, package, directory, has_source):
        require(re.fullmatch(r"files/p6-(?:process-death|installer-probe)/[a-f0-9]{32}", directory), "Unsafe cleanup path")
        names = [base + suffix for base in ["evidence.json", "error.json", "enable-restore.json"] for suffix in ["", ".bak", ".new"]]
        if has_source:
            names.append("fixture.apk")
        for name in names:
            self.shell("run-as", package, "rm", "-f", directory + "/" + name)
        output = self.shell("run-as", package, "rmdir", directory)
        require(not output.strip(), f"Private case contains unexpected files: {output}")


def wait_session_absent(device, session_id, output):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        text, sessions = device.sessions()
        if session_id not in sessions:
            (output / "sessions-after-death.txt").write_text(text, encoding="utf-8")
            return sessions
        time.sleep(0.5)
    raise RuntimeError(f"Owned platform session {session_id} was not abandoned")


def run(args):
    device = Device(args.adb, args.serial)
    require(device.run("get-state").strip() == b"device", "Device is not connected")
    require(int(device.shell("getprop", "ro.build.version.sdk").strip()) >= 24, "Unsupported API")
    fixture = SPIKE if args.mode == "plugin-main" else PERFORMANCE
    device.fixture_absent(fixture)
    source = args.fixture.read_bytes()
    if args.mode == "plugin-main":
        require(args.authorizer in {"root", "shizuku"}, "Plugin main death requires a privileged authorizer")
        require(0 < len(source) <= 1024 * 1024, "Expected the small fixed spike-v1 fixture")
    else:
        require(100 * 1024 * 1024 <= len(source) <= 102 * 1024 * 1024, "Expected the dedicated 100 MiB performance fixture")
    history = device.document(PLUGIN, "no_backup/installation-history/history.json")
    if history is not None:
        require(len(history.get("entries", [])) < 200, "A probe must not evict existing installation history")
    case_id = uuid.uuid4().hex
    output = REPO / "build" / "process-death-acceptance" / (time.strftime("%Y%m%d-%H%M%S") + "-" + case_id)
    output.mkdir(parents=True)
    plugin_dir = "files/p6-process-death/" + case_id
    host_dir = "files/p6-installer-probe/" + case_id
    baseline_text, baseline = device.sessions()
    (output / "sessions-before.txt").write_text(baseline_text, encoding="utf-8")
    result = {"caseId": case_id, "serial": args.serial, "mode": args.mode, "authorizer": args.authorizer,
              "fixturePackage": fixture, "sourceSha256": hashlib.sha256(source).hexdigest(),
              "baselinePlatformSessionIds": sorted(baseline), "passed": False}
    (output / "restore-plan.json").write_text(json.dumps({**result, "pluginDirectory": plugin_dir,
        "hostDirectory": host_dir if args.mode == "host" else None, "packageRemovalPermitted": False,
        "killScope": "only the exact recorded PID, UID, process name and start time"}, indent=2), encoding="utf-8")
    plugin_created = host_created = started = enable_prepare_requested = False
    try:
        device.create_case(PLUGIN, plugin_dir, args.fixture if args.mode == "plugin-main" else None)
        plugin_created = True
        if args.mode == "plugin-main":
            device.launch(PLUGIN_COMPONENT, case_id, "start", args.authorizer)
            started = True
            before = device.wait_document(PLUGIN, plugin_dir, "ready-for-main-death", seconds=60)
            require(before["fixturePackage"] == fixture and before["sourceSha256"] == result["sourceSha256"], "Probe source identity differs")
            require(before["bytesWritten"] > 0 and before["createCount"] == 1 and before["commitCount"] == 0 and before["foregroundObserved"], "Missing real write evidence")
            session_id = before["platformSessionId"]
            text, sessions = device.sessions()
            require(session_id not in baseline and session_id in sessions, "The recorded new platform session is not active")
            require(re.search(r"mInstallerUid=" + str(before["privilegedUid"]) + r"\b", sessions[session_id]), "Platform session owner does not match the bound privileged UID")
            (output / "sessions-writing.txt").write_text(text, encoding="utf-8")
            identity = device.identity(PLUGIN, before["seedPid"], PLUGIN)
            require(identity["uid"] == before["pluginUid"], "Plugin UID differs")
            result["killedProcess"] = identity
            result["beforeDeath"] = before
            (output / "death-point.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
            result["deathMechanism"] = device.kill_owned(PLUGIN, identity, PLUGIN_COMPONENT, case_id, args.authorizer)
            after_sessions = wait_session_absent(device, session_id, output)
            result["platformSessionAbandoned"] = True
            result["baselineSessionIdsStillPresent"] = sorted(set(baseline) & set(after_sessions))
            device.launch(PLUGIN_COMPONENT, case_id, "recover", args.authorizer)
            recovered = device.wait_document(PLUGIN, plugin_dir, "recovered-without-replay")
            require(recovered["recoverPid"] != identity["pid"] and recovered["recoveredHistory"] == "cancelled" and recovered["recoveredInterrupted"], "No real cold recovery cancellation")
            result["recovered"] = recovered
        else:
            device.create_case(HOST, host_dir, args.fixture)
            host_created = True
            enable_prepare_requested = True
            device.launch(HOST_COMPONENT, case_id, "prepare", args.authorizer)
            result["enablePrepared"] = device.wait_enable_journal(host_dir, "prepared")
            (output / "enable-prepared.json").write_text(json.dumps(result["enablePrepared"], indent=2), encoding="utf-8")
            device.launch(HOST_COMPONENT, case_id, "host-death", args.authorizer)
            started = True
            host = device.wait_document(HOST, host_dir, "ready-for-host-death", seconds=60)
            require(host["fixturePackage"] == fixture and host["sourceSha256"] == result["sourceSha256"], "Host fixture identity differs")
            require(host["sourceBytesSent"] == 512 * 1024 and host["sourceProcessPid"] != host["pid"], "The held source is not independent of the caller")
            device.launch(PLUGIN_COMPONENT, case_id, "watch-host", args.authorizer)
            before = device.wait_document(PLUGIN, plugin_dir, "ready-for-host-death")
            require(256 * 1024 <= before["stagedBytes"] <= 512 * 1024 and not before["platformSessionCreated"] and before["sourceOrigin"] == "host", "No real host-source staging evidence")
            require(before["stagedSha256"] == hashlib.sha256(source[:before["stagedBytes"]]).hexdigest(), "Actual staged bytes differ from the fixed source prefix")
            identity = device.identity(HOST, host["pid"], HOST + ":installer_process_probe")
            require(identity["uid"] == host["uid"], "Official host caller UID differs")
            result["killedProcess"] = identity
            result["hostBeforeDeath"] = host
            result["beforeDeath"] = before
            (output / "death-point.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
            result["deathMechanism"] = device.kill_owned(HOST, identity, HOST_COMPONENT, case_id, args.authorizer)
            cancelled = device.wait_document(PLUGIN, plugin_dir, "host-death-cancelled")
            require(cancelled["errorCode"] == "CANCELLED" and cancelled["stagingRemoved"], "The real host Binder death did not cancel and clean staging")
            result["cancelled"] = cancelled
        device.fixture_absent(fixture)
        result["fixtureAbsent"] = True
        result["passed"] = True
    except Exception as failure:
        result["failure"] = str(failure)
    finally:
        try:
            if plugin_created and started and device.document(PLUGIN, plugin_dir + "/evidence.json") is not None:
                device.launch(PLUGIN_COMPONENT, case_id, "cleanup" if args.mode == "plugin-main" else "cleanup-host", args.authorizer)
                result["pluginCleanup"] = device.wait_document(PLUGIN, plugin_dir, "cleaned", ignore_error=True)
            if host_created and device.document(HOST, host_dir + "/evidence.json") is not None:
                device.launch(HOST_COMPONENT, case_id, "cleanup", args.authorizer)
                result["hostCleanup"] = device.wait_document(HOST, host_dir, "cleaned", ignore_error=True)
            device.fixture_absent(fixture)
        except Exception as failure:
            result["passed"] = False
            result["cleanupFailure"] = str(failure)
        finally:
            # Preference restoration is independent of source/session cleanup, including failure.
            if enable_prepare_requested:
                try:
                    device.launch(HOST_COMPONENT, case_id, "restore", args.authorizer)
                    restored = device.wait_enable_journal(host_dir, "restored")
                    require(restored["pending"] is False and restored["restoredPresent"] == restored["originalPresent"] and
                            restored["restoredEnabled"] == restored["originalEnabled"], "Original host enable preference was not restored")
                    prepared = result.get("enablePrepared")
                    if prepared:
                        require(all(restored[key] == prepared[key] for key in ["originalPresent", "originalEnabled", "hostUid", "fingerprints"]), "Enable restoration journal changed identity or baseline")
                    result["enableRestored"] = restored
                except Exception as failure:
                    result["passed"] = False
                    result["enableRestoreFailure"] = str(failure)
        if result["passed"]:
            try:
                require(result.get("pluginCleanup", {}).get("ownedHistoryRemoved"), "Owned history cleanup was not confirmed")
                if host_created:
                    device.remove_case(HOST, host_dir, has_source=True)
                device.remove_case(PLUGIN, plugin_dir, has_source=args.mode == "plugin-main")
                result["privateCasesRemoved"] = True
            except Exception as failure:
                result["passed"] = False
                result["cleanupFailure"] = str(failure)
        (output / "result.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps({"passed": result["passed"], "evidence": str(output), "failure": result.get("failure"), "cleanupFailure": result.get("cleanupFailure")}, indent=2))
    return 0 if result["passed"] else 1


def self_test():
    sample = "Active install sessions:\n  Active Session 24:\n    mInstallerUid=0\n    Active Child Session 25:\n      mInstallerUid=0\nHistorical install sessions:\n  Session 9:\n"
    _, entries = active_sessions(sample)
    require(set(entries) == {24, 25} and "Session 9" not in str(entries), "Historical session parser guard failed")
    require(active_sessions("Active install sessions:\n\nHistorical install sessions:\n")[1] == {}, "Empty active baseline failed")
    require(active_sessions("Active install sessions:\n\nFinalized install sessions:\n  Session 17:\n\nHistorical install sessions:\n")[1] == {}, "Finalized sessions are not active")
    for invalid in ["permission denied", "Active install sessions:\nunknown\nHistorical install sessions:\n", "Active install sessions:\n"]:
        try:
            active_sessions(invalid)
        except RuntimeError:
            pass
        else:
            raise RuntimeError("Malformed platform session evidence was accepted")
    print("process-death driver self-test passed")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial")
    parser.add_argument("--mode", choices=["plugin-main", "host"], default="plugin-main")
    parser.add_argument("--authorizer", choices=["none", "shizuku", "root"], default="shizuku")
    parser.add_argument("--adb", default=str(Path(os.environ.get("ANDROID_HOME", "E:/.android/sdk")) / "platform-tools/adb.exe"))
    parser.add_argument("--fixture", type=Path)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    if arguments.self_test:
        self_test()
    else:
        parser.error("--serial is required") if not arguments.serial else None
        if arguments.fixture is None:
            if arguments.mode == "host":
                parser.error("--fixture must name the generated 100 MiB performance APK for host mode")
            arguments.fixture = REPO / "app/src/androidTest/assets/fixture-v1.apk"
        sys.exit(run(arguments))
