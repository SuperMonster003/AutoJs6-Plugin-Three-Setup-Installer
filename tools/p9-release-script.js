/* P9 acceptance through the official host UID and public Rhino API.
 * The desktop harness provides fixed APKs only on its task-owned API 31 Dhizuku AVD.
 * No owner, default-handler policy or authorization setting is changed.
 */
(function () {
    var c = P9_ACCEPTANCE;
    function require(value, message) { if (!value) throw new Error(message); }
    require(c && c.ownedAvd === 'Three_Setup_Dhizuku_P8_API31', 'Dedicated AVD opt-in required');
    require(c.token && /^[0-9a-f]{32}$/.test(c.token), 'Invalid acceptance token');
    var authorizer = c.authorizer || 'dhizuku';
    require(authorizer === 'dhizuku' || authorizer === 'shizuku', 'Unsupported fixture authorizer');
    require(String(context.getPackageName()) === 'org.autojs.autojs6', 'Official host required');
    require(android.os.Build.VERSION.SDK_INT === 31 && String(android.os.Build.MODEL) === 'sdk_gphone64_x86_64', 'Dedicated API 31 emulator required');
    require(Math.floor(android.os.Process.myUid() / 100000) === 0, 'Primary test user required');
    var directory = String(context.getFilesDir()) + '/p9-release-' + c.token;
    var reportPath = directory + '/result.json';
    var packageName = 'io.github.supermonster003.autojs6.installer.advanced.fixture';
    var hashes = {
        'v1.apk': 'bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0',
        'v2.apk': '201ae190eeee0668b0fba8dabea30cb67a0c97f21a21370fba9f3278b402409e',
        'v2-other.apk': '171e52e90eac7b93d9406732fd7960ce47eea0c500a23f6089c7eb3ef8e8ad3e',
        'unsigned.apk': 'e593e18c986c2782d0200113ac4e193e60e6555c9a7c3ae4b8536b3f4afad861'
    };
    require(!files.exists(reportPath), 'A previous run must not be replayed');
    function installedVersion() {
        try { return Number(context.getPackageManager().getPackageInfo(packageName, 8192).versionCode); }
        catch (failure) {
            require(String(failure).indexOf('NameNotFoundException') >= 0, 'Package absence is not proved');
            return null;
        }
    }
    function sha256(path) {
        var bytes = java.security.MessageDigest.getInstance('SHA-256').digest(files.readBytes(path));
        var result = '';
        for (var i = 0; i < bytes.length; i++) result += ('0' + (bytes[i] & 255).toString(16)).slice(-2);
        return result;
    }
    function options(extra) {
        var result = { authorizer: authorizer, interaction: 'silent', timeout: 30000 };
        Object.keys(extra || {}).forEach(function (key) { result[key] = extra[key]; });
        return result;
    }
    var report = { token: c.token, hostUid: android.os.Process.myUid(), hostPid: android.os.Process.myPid(),
        ok: false, rejected: [], stages: [], terminalCount: 0 };
    function rejected(file, extra, expected) {
        var failure;
        try { installer.install(directory + '/' + file, options(extra)); } catch (error) { failure = error; }
        require(failure instanceof installer.InstallerError && failure.code === expected,
            'Expected ' + expected + ', got ' + String(failure));
        report.rejected.push({ file: file, options: extra, code: failure.code });
    }
    var installStarted = false;
    try {
        require(installedVersion() === null, 'Fixture or retained data already exists');
        Object.keys(hashes).forEach(function (file) {
            require(sha256(directory + '/' + file) === hashes[file], 'Unexpected fixture bytes: ' + file);
        });
        require(installer.isAvailable(), 'Plugin is unavailable');
        report.status = installer.status;
        require(report.status.contractVersion === 3, 'V3 must be negotiated with the live plugin');
        var state = installer.authorizer.state(authorizer);
        require(state.available && state.running && state.granted, 'The selected authorizer must already be authorized');
        if (authorizer === 'dhizuku') {
            rejected('v1.apk', { grantAllRequestedPermissions: true }, 'AUTHORIZER_REQUIRED');
            rejected('v1.apk', { dexopt: 'speed' }, 'AUTHORIZER_REQUIRED');
        }
        rejected('v1.apk', { requestUpdateOwnership: true }, 'INVALID_ARGUMENT');
        rejected('v1.apk', { packageSource: 'other' }, 'INVALID_ARGUMENT');
        require(installedVersion() === null, 'Unsupported choices installed a package');
        installStarted = true;
        var session = installer.session(directory + '/v1.apk', options({ grantAllRequestedPermissions: authorizer === 'shizuku',
            requestUpdateOwnership: false, dexopt: authorizer === 'shizuku' ? 'speed' : 'none', installReason: 'user' }));
        session.on('stage', function (stage) { report.stages.push(stage); });
        session.on('complete', function () { report.terminalCount++; });
        report.install = session.wait(30000);
        require(report.install.ok && report.install.versionCode === 1 && installedVersion() === 1, 'V3 session install failed');
        require(session.state === 'completed' && report.terminalCount === 1 && session.cancel() === false, 'Session terminal events differ');
        require(report.install.updateOwner === undefined, 'Unrequested ownership results must be absent');
        if (authorizer === 'shizuku') {
            require(report.install.dexopt && report.install.dexopt.filter === 'speed' && report.install.dexopt.status === 'accepted',
                'The optional compilation result did not survive the public host API');
            require(report.stages.indexOf('optimizing') >= 0, 'Compilation stage was not delivered');
            require(context.getPackageManager().checkPermission('android.permission.READ_CALENDAR', packageName) === 0,
                'The runtime permission was not actually granted');
            report.grantedRuntimePermission = 'android.permission.READ_CALENDAR';
        } else require(report.install.dexopt === undefined, 'Unrequested compilation results must be absent');
        report.update = installer.install(directory + '/v2.apk', options({ installReason: 'user' }));
        require(report.update.ok && installedVersion() === 2, 'Same-signer update failed');
        rejected('v2-other.apk', {}, 'BLOCKED_BY_POLICY');
        rejected('unsigned.apk', {}, 'BLOCKED_BY_POLICY');
        require(installedVersion() === 2, 'Rejected sources changed the installed package');
        report.uninstall = installer.uninstall(packageName, options());
        require(report.uninstall.ok && installedVersion() === null, 'Owned fixture was not uninstalled');
        installStarted = false;
        Object.keys(hashes).forEach(function (file) {
            require(sha256(directory + '/' + file) === hashes[file], 'Source changed: ' + file);
        });
        report.ok = true;
    } catch (failure) {
        report.error = String(failure) + (failure.stack ? '\n' + failure.stack : '');
    } finally {
        // Desktop cleanup independently checks ownership when an install outcome is uncertain.
        report.installOutcomeNeedsInspection = installStarted;
        files.write(reportPath, JSON.stringify(report, null, 2));
    }
})();
