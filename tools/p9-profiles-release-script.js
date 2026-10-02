/* Public Rhino acceptance for source profiles. The desktop harness seeds and restores
 * only its dedicated API 31 AVD's profile document, and supplies these fixed sources.
 */
(function () {
    var c = P9_PROFILE_ACCEPTANCE;
    function require(value, message) { if (!value) throw new Error(message); }
    require(c && c.ownedAvd === 'Three_Setup_Dhizuku_P8_API31', 'Dedicated AVD opt-in required');
    require(c.token && /^[0-9a-f]{32}$/.test(c.token), 'Invalid acceptance token');
    require(String(context.getPackageName()) === 'org.autojs.autojs6', 'Official host required');
    require(android.os.Build.VERSION.SDK_INT === 31 && String(android.os.Build.MODEL) === 'sdk_gphone64_x86_64', 'Dedicated API 31 emulator required');
    require(Math.floor(android.os.Process.myUid() / 100000) === 0, 'Primary test user required');
    var directory = String(context.getFilesDir()) + '/p9-profiles-' + c.token;
    var reportPath = directory + '/result.json';
    var packageName = 'io.github.supermonster003.autojs6.installer.advanced.fixture';
    var hashes = {
        'profile-delete.apk': 'bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0',
        'explicit-retain.apk': '201ae190eeee0668b0fba8dabea30cb67a0c97f21a21370fba9f3278b402409e',
        'other-signer.apk': '171e52e90eac7b93d9406732fd7960ce47eea0c500a23f6089c7eb3ef8e8ad3e',
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
    var report = { token: c.token, hostUid: android.os.Process.myUid(), hostPid: android.os.Process.myPid(),
        ok: false, rejected: [], stages: [], terminalCount: 0 };
    var installationStarted = false;
    try {
        require(installedVersion() === null, 'Fixture or retained data already exists');
        Object.keys(hashes).forEach(function (file) {
            require(sha256(directory + '/' + file) === hashes[file], 'Unexpected source bytes: ' + file);
        });
        require(installer.isAvailable(), 'Plugin is unavailable');
        report.status = installer.status;
        require(report.status.contractVersion === 3, 'Live V3 negotiation required');
        var state = installer.authorizer.state('dhizuku');
        require(state.available && state.running && state.granted, 'Existing Dhizuku authorization required');
        installationStarted = true;
        // Authorizer, deleteSource and all per-package options are absent here. The first
        // enabled script/prefix profile supplies them; the later Root profile must not stack.
        var session = installer.session(directory + '/profile-delete.apk', { interaction: 'silent', timeout: 60000 });
        session.on('stage', function (stage) { report.stages.push(stage); });
        session.on('complete', function () { report.terminalCount++; });
        report.install = session.wait(60000);
        require(report.install.ok && report.install.authorizer === 'dhizuku' && installedVersion() === 1, 'Profile authorizer or installation differs');
        require(report.install.sourceDeleteRequested === true && report.install.sourceDeleted === true &&
            !files.exists(directory + '/profile-delete.apk'), 'Host did not honor profile source deletion');
        require(session.state === 'completed' && report.terminalCount === 1, 'Unexpected terminal events');
        report.explicitOptions = { interaction: 'silent', timeout: 60000, authorizer: 'dhizuku', deleteSource: false,
            installer: null, installReason: null, packageSource: null,
            grantAllRequestedPermissions: false, requestUpdateOwnership: false, dexopt: 'none' };
        report.update = installer.install(directory + '/explicit-retain.apk', report.explicitOptions);
        require(report.update.ok && installedVersion() === 2 && report.update.sourceDeleteRequested === false &&
            report.update.sourceDeleted === false, 'Explicit false did not retain the source');
        require(sha256(directory + '/explicit-retain.apk') === hashes['explicit-retain.apk'], 'Explicitly retained source changed');
        ['other-signer.apk', 'unsigned.apk'].forEach(function (file) {
            var failure;
            try { installer.install(directory + '/' + file, { interaction: 'silent', timeout: 60000 }); }
            catch (error) { failure = error; }
            require(failure instanceof installer.InstallerError && failure.code === 'BLOCKED_BY_POLICY', 'Profile bypassed mandatory signature policy');
            require(sha256(directory + '/' + file) === hashes[file], 'Rejected source was removed');
            report.rejected.push({ file: file, code: failure.code });
        });
        require(installedVersion() === 2, 'Rejected sources changed the installed package');
        report.uninstall = installer.uninstall(packageName, { authorizer: 'dhizuku', interaction: 'silent', timeout: 60000 });
        require(report.uninstall.ok && installedVersion() === null, 'Owned fixture was not uninstalled');
        installationStarted = false;
        report.ok = true;
    } catch (failure) {
        report.error = String(failure) + (failure.stack ? '\n' + failure.stack : '');
    } finally {
        report.installOutcomeNeedsInspection = installationStarted;
        files.write(reportPath, JSON.stringify(report, null, 2));
    }
})();
