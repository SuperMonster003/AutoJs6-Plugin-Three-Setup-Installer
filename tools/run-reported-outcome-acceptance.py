#!/usr/bin/env python3
"""Reported-outcome acceptance: the plugin's result page and the result returned to AutoJs6 must follow the
package's actual state after the platform session ends.

Temporary AVD only (serial emulator-*): the plugin dialog, the settings page and the system confirmation are
tapped through uiautomator, the confirmation is dismissed with the Back key. The debug AutoJs6 host and the
plugin must be signed by the same certificate; this driver installs both.

Scenarios:
  self-update  AutoJs6 asks the plugin (installer.install, authorizer none, interaction dialog) to reinstall
               the host's own APK. The system replaces AutoJs6 and kills it while the plugin is still waiting
               for the session result (the caller's Binder dies after commit).
  oem-abort    AutoJs6 asks the plugin to install the fixture while the plugin has no "install unknown apps"
               permission: the plugin opens the settings page (the switch is turned on here), then the system
               confirmation. Like the ColorOS installer, which installs the APK through its own session, the
               same APK is installed by the shell and the confirmation is then rejected with Back
               (INSTALL_FAILED_ABORTED: User rejected permissions).

Each scenario prints one PASS / FAIL line with the title of the plugin's result page, the error code shown
there, the code AutoJs6 received (oem-abort) and the package's lastUpdateTime before / after.

Example (from the plugin repository):
  py -3.12 -I tools/run-reported-outcome-acceptance.py -s emulator-5640 --work build/reported-outcome \\
      --host ../AutoJs6/app/build/outputs/apk/app/debug/autojs6-v6.8.1-x86_64.apk \\
      --plugin app/build/outputs/apk/debug/<plugin>.apk self-update oem-abort
zh-CN: 安装结果验收: 插件结果页与回传给 AutoJs6 的结果须与会话结束后包的实际状态一致. 仅用于临时 AVD (emulator-*):
插件对话框, 设置页与系统确认用 uiautomator 查找后点按, 系统确认用返回键关闭. self-update 让宿主经插件重装自身;
oem-abort 模拟 ColorOS 安装器用自己的会话装上后再中止插件的会话. 每个场景输出一行 PASS / FAIL 与关键数值.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree

PLUGIN = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
HOST = "org.autojs.autojs6"
RUNNER = HOST + "/org.autojs.autojs.external.open.RunIntentActivity"
FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture"
REPO = pathlib.Path(__file__).resolve().parent.parent
FIXTURE_APK = REPO / "app/src/androidTest/assets/fixture-v2.apk"
HOST_FILES = "/sdcard/Android/data/%s/files" % HOST
TITLES = {
    "Installation successful": "success",
    "Installation cancelled": "cancelled",
    "Installation failed": "failed",
    "Installation interrupted": "interrupted",
}
# A UI script keeps AutoJs6 in the foreground, as the About page does during an app update, so the plugin may
# bring up its dialog.
SCRIPT = r'''"ui";
ui.layout(<vertical padding="24"><text text="reported outcome acceptance" /></vertical>);
threads.start(function () {
    var output = new java.io.File(context.getExternalFilesDir(null), "__RECEIPT__").getAbsolutePath();
    var receipt = {started: Date.now()};
    files.write(output, JSON.stringify(receipt));
    Packages.org.autojs.autojs.core.plugin.center.PluginEnableStore.INSTANCE.setEnabled(context, "__PLUGIN__", true);
    try {
        var result = installer.install(__SOURCE__, {authorizer: "none", interaction: "dialog", deleteSource: false});
        receipt.ok = true;
        receipt.result = JSON.parse(JSON.stringify(result));
    } catch (e) {
        receipt.ok = false;
        receipt.code = e.code ? String(e.code) : null;
        receipt.error = String(e);
    }
    receipt.complete = true;
    files.write(output, JSON.stringify(receipt));
});
'''


class Device:
    def __init__(self, serial: str, work: pathlib.Path):
        if not re.fullmatch(r"emulator-\d+", serial):
            raise SystemExit("This driver taps through uiautomator and is limited to temporary AVDs (emulator-*)")
        self.serial = serial
        self.work = work

    def adb(self, *command: str, check: bool = True, timeout: int = 300) -> str:
        result = subprocess.run(["adb", "-s", self.serial, *command], capture_output=True, text=True,
                                encoding="utf-8", errors="replace", timeout=timeout)
        output = (result.stdout + result.stderr).strip()
        if check and result.returncode:
            raise RuntimeError("adb %s failed: %s" % (" ".join(command), output[-400:]))
        return output

    def shell(self, command: str, check: bool = True, timeout: int = 300) -> str:
        return self.adb("shell", command, check=check, timeout=timeout)

    def last_update(self, package: str) -> str | None:
        match = re.search(r"lastUpdateTime=([0-9-]+ [0-9:]+)", self.shell("dumpsys package %s" % package, check=False))
        return match.group(1) if match else None

    def version_code(self, package: str) -> str | None:
        match = re.search(r"versionCode=(\d+)", self.shell("dumpsys package %s" % package, check=False))
        return match.group(1) if match else None

    def nodes(self) -> list[dict]:
        self.shell("uiautomator dump /data/local/tmp/reported-outcome.xml", check=False, timeout=60)
        raw = self.adb("exec-out", "cat", "/data/local/tmp/reported-outcome.xml", check=False)
        start = raw.find("<?xml")
        if start < 0:
            return []
        try:
            root = ElementTree.fromstring(raw[start:raw.rfind(">") + 1])
        except ElementTree.ParseError:
            return []
        found = []
        for node in root.iter("node"):
            bounds = re.findall(r"\d+", node.get("bounds", ""))
            if len(bounds) != 4:
                continue
            left, top, right, bottom = map(int, bounds)
            found.append(dict(text=node.get("text", ""), package=node.get("package", ""), id=node.get("resource-id", ""),
                              clickable=node.get("clickable") == "true", enabled=node.get("enabled") == "true",
                              center=((left + right) // 2, (top + bottom) // 2)))
        return found

    def tap(self, node: dict) -> None:
        self.shell("input tap %d %d" % node["center"])

    def wait(self, predicate, timeout: float, step: float = 0.7):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            value = predicate(self.nodes())
            if value:
                return value
            time.sleep(step)
        return None


def visible_texts(device: Device) -> list[str]:
    return [n["text"][:40] for n in device.nodes() if n["text"]][:12]


def plugin_install_button(nodes):
    return next((n for n in nodes if n["package"] == PLUGIN and n["clickable"] and n["enabled"]
                 and n["text"].strip().lower() == "install"), None)


def system_confirm_button(nodes):
    return next((n for n in nodes if "packageinstaller" in n["package"] and n["id"] == "android:id/button1" and n["enabled"]), None)


def system_confirmation(nodes):
    return any("packageinstaller" in n["package"] for n in nodes) and system_confirm_button(nodes)


def unknown_source_switch(nodes):
    return next((n for n in nodes if n["package"] == "com.android.settings" and n["text"] == "Allow from this source"), None)


def plugin_result(nodes):
    texts = [n["text"] for n in nodes if n["package"] == PLUGIN and n["text"]]
    title = next((TITLES[t] for t in texts if t in TITLES), None)
    if title is None:
        return None
    codes = [m.group(1) for t in texts for m in [re.search(r"Error code:?\s*([A-Z_]+)", t)] if m]
    return dict(title=title, code=codes[0] if codes else None)


def start_script(device: Device, receipt: str, source: str) -> None:
    script = SCRIPT.replace("__RECEIPT__", receipt).replace("__PLUGIN__", PLUGIN).replace("__SOURCE__", json.dumps(source))
    device.shell("rm -f %s/%s" % (HOST_FILES, receipt), check=False)
    device.shell("am force-stop %s" % HOST, check=False)
    launcher =device.shell("cmd package resolve-activity --brief -c android.intent.category.LAUNCHER %s" % HOST).splitlines()[-1]
    device.shell("am start -W -n %s" % launcher, check=False)
    time.sleep(2)
    device.shell(shlex.join(["am", "start", "-n", RUNNER, "--es", "script", script]))


def read_receipt(device: Device, receipt: str, timeout: float) -> dict | None:
    deadline = time.monotonic() + timeout
    value = None
    while time.monotonic() < deadline:
        try:
            value = json.loads(device.shell("cat %s/%s" % (HOST_FILES, receipt), check=False))
            if value.get("complete"):
                return value
        except json.JSONDecodeError:
            pass
        time.sleep(1)
    return value


def prepare(device: Device, args) -> None:
    device.adb("install", "-r", "-d", "-g", str(args.host))
    device.adb("install", "-r", "-d", str(args.plugin))
    device.shell("appops set %s MANAGE_EXTERNAL_STORAGE allow" % HOST, check=False)
    device.shell("mkdir -p %s" % HOST_FILES, check=False)
    device.shell("am force-stop %s" % PLUGIN, check=False)


def scenario_self_update(device: Device, args) -> bool:
    device.shell("appops set %s REQUEST_INSTALL_PACKAGES allow" % PLUGIN)
    device.adb("push", str(args.host), HOST_FILES + "/self-update.apk")
    before = device.last_update(HOST)
    start_script(device, "self-update.json", HOST_FILES + "/self-update.apk")
    # A debug plugin hashes and inspects the 60 MB host APK slowly on a cold AVD.
    button = device.wait(plugin_install_button, 300)
    if not button:
        print("FAIL self-update: the plugin dialog did not offer Install; screen: %s" % visible_texts(device))
        return False
    device.tap(button)
    confirm = device.wait(system_confirmation, 60)
    if not confirm:
        print("FAIL self-update: no system confirmation")
        return False
    device.tap(confirm)
    result = device.wait(plugin_result, 90)
    after = device.last_update(HOST)
    replaced = before != after
    ok = bool(result) and replaced and result["title"] == "success"
    print("%s self-update: page=%s code=%s host lastUpdateTime %s -> %s (replaced=%s)" % (
        "PASS" if ok else "FAIL", result and result["title"], result and result["code"], before, after, replaced))
    return ok


def scenario_oem_abort(device: Device, args) -> bool:
    device.shell("pm uninstall %s" % FIXTURE, check=False)
    device.shell("appops set %s REQUEST_INSTALL_PACKAGES default" % PLUGIN)
    device.adb("push", str(FIXTURE_APK), HOST_FILES + "/oem-abort.apk")
    device.adb("push", str(FIXTURE_APK), "/data/local/tmp/reported-outcome-fixture.apk")
    start_script(device, "oem-abort.json", HOST_FILES + "/oem-abort.apk")
    button = device.wait(plugin_install_button, 180)
    if not button:
        print("FAIL oem-abort: the plugin dialog did not offer Install; screen: %s" % visible_texts(device))
        return False
    device.tap(button)
    switch = device.wait(unknown_source_switch, 60)
    if not switch:
        print("FAIL oem-abort: the plugin did not open the install-unknown-apps page")
        return False
    device.tap(switch)
    time.sleep(1)
    device.shell("input keyevent 4")
    if not device.wait(system_confirmation, 60):
        print("FAIL oem-abort: no system confirmation after the permission was granted")
        return False
    before = device.last_update(FIXTURE)
    installed = device.shell("pm install -r -d /data/local/tmp/reported-outcome-fixture.apk", check=False)
    after_shell = device.last_update(FIXTURE)
    device.shell("input keyevent 4")
    result = device.wait(plugin_result, 60)
    receipt = read_receipt(device, "oem-abort.json", 30) or {}
    after = device.last_update(FIXTURE)
    host_code = "ok" if receipt.get("ok") else receipt.get("code")
    ok = bool(result) and result["title"] == "success" and receipt.get("ok") is True and after is not None
    print("%s oem-abort: page=%s code=%s host=%s fixture lastUpdateTime %s -> %s (shell %s, versionCode %s)" % (
        "PASS" if ok else "FAIL", result and result["title"], result and result["code"], host_code, before, after,
        "Success" if "Success" in installed else installed[-80:], device.version_code(FIXTURE)))
    device.shell("rm -f /data/local/tmp/reported-outcome-fixture.apk /data/local/tmp/reported-outcome.xml", check=False)
    return ok


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("-s", "--serial", required=True)
    parser.add_argument("--work", type=pathlib.Path, required=True)
    parser.add_argument("--host", type=pathlib.Path, required=True, help="debug AutoJs6 APK for the AVD's ABI")
    parser.add_argument("--plugin", type=pathlib.Path, required=True, help="plugin APK signed like the host")
    parser.add_argument("scenarios", nargs="+", choices=["self-update", "oem-abort"])
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    device = Device(args.serial, args.work)
    prepare(device, args)
    results = []
    for name in args.scenarios:
        results.append({"self-update": scenario_self_update, "oem-abort": scenario_oem_abort}[name](device, args))
        device.shell("am force-stop %s" % PLUGIN, check=False)
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
