/* P8 acceptance through the official host UID and public Rhino API.
 * The desktop driver supplies P8_ACCEPTANCE only on its own dedicated Dhizuku AVD.
 * No user package, device owner, permission, or competing policy is changed here.
 */
(function () {
    var c = P8_ACCEPTANCE;
    function require(value, message) { if (!value) throw new Error(message); }
    require(c && c.ownedAvd === 'Three_Setup_Dhizuku_P8_API31', 'Dedicated AVD opt-in required');
    require(c.token && /^[0-9a-f]{32}$/.test(c.token), 'Invalid acceptance token');
    require(String(context.getPackageName()) === 'org.autojs.autojs6', 'Official host required');
    require(android.os.Build.VERSION.SDK_INT === 31 && String(android.os.Build.MODEL) === 'sdk_gphone64_x86_64', 'Dedicated API 31 emulator required');
    require(Math.floor(android.os.Process.myUid() / 100000) === 0, 'Primary test user required');
    var directory = String(context.getFilesDir()) + '/p8-release-' + c.token;
    var reportPath = directory + '/result.json';
    var fixture = directory + '/fixture-v1.apk';
    var fixturePackage = 'io.github.supermonster003.autojs6.installer.spike.fixture';
    require(!files.exists(reportPath), 'A previous run must not be replayed');
    function installed() {
        try { context.getPackageManager().getPackageInfo(fixturePackage, 8192); return true; }
        catch (failure) {
            require(String(failure).indexOf('NameNotFoundException') >= 0, 'Package absence is not proved');
            return false;
        }
    }
    function sha256(path) {
        var bytes = java.security.MessageDigest.getInstance('SHA-256').digest(files.readBytes(path));
        var result = '';
        for (var i = 0; i < bytes.length; i++) result += ('0' + (bytes[i] & 255).toString(16)).slice(-2);
        return result;
    }
    var report = { token: c.token, hostUid: android.os.Process.myUid(), hostPid: android.os.Process.myPid(), ok: false };
    var policyStarted = false;
    var installStarted = false;
    try {
        require(!installed(), 'Fixture or retained data already exists');
        require(sha256(fixture) === 'fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69', 'Unexpected fixture bytes');
        require(installer.isAvailable(), 'Plugin is unavailable');
        report.status = installer.status;
        require(report.status.contractVersion === 2, 'V2 must be negotiated with the live plugin');
        var state = installer.authorizer.state('dhizuku');
        require(state.available && state.running && state.granted, 'Dhizuku must already be authorized');
        report.initialDefault = installer.isDefault();
        require(report.initialDefault === false, 'Dedicated default baseline changed');
        policyStarted = true;
        report.persistentSet = installer.setDefault(true, { authorizer: 'dhizuku', mode: 'persistent' });
        require(report.persistentSet === true && installer.isDefault(), 'Persistent set did not select the plugin');
        report.persistentClear = installer.setDefault(false, { authorizer: 'dhizuku', mode: 'persistent' });
        require(report.persistentClear === true && !installer.isDefault(), 'Persistent clear did not restore the baseline');
        policyStarted = false;
        installStarted = true;
        report.install = installer.install(fixture, { authorizer: 'dhizuku', interaction: 'silent', timeout: 30000 });
        require(report.install.ok && report.install.packageName === fixturePackage && report.install.versionCode === 1 &&
            report.install.authorizer === 'dhizuku' && report.install.interaction === 'silent', 'Unexpected install result');
        var info = context.getPackageManager().getPackageInfo(fixturePackage, 0);
        require(Number(info.versionCode) === 1, 'Actual installed version differs');
        report.actualInstaller = String(context.getPackageManager().getInstallerPackageName(fixturePackage));
        require(report.actualInstaller === 'com.rosan.dhizuku', 'Installer attribution differs from the owner');
        report.uninstall = installer.uninstall(fixturePackage, { authorizer: 'dhizuku', interaction: 'silent', timeout: 30000 });
        require(report.uninstall.ok && !installed(), 'Owned fixture was not uninstalled');
        installStarted = false;
        require(sha256(fixture) === 'fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69', 'Source changed');
        report.ok = true;
    } catch (failure) {
        report.error = String(failure) + (failure.stack ? '\n' + failure.stack : '');
    } finally {
        if (policyStarted) {
            try { report.policyCleanup = installer.setDefault(false, { authorizer: 'dhizuku', mode: 'persistent' }); }
            catch (failure) { report.policyCleanupError = String(failure); }
        }
        // Desktop cleanup independently verifies ownership if the install outcome is uncertain.
        report.installOutcomeNeedsInspection = installStarted;
        files.write(reportPath, JSON.stringify(report, null, 2));
    }
})();
