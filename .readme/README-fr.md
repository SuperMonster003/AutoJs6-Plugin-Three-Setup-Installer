<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Installe, met à jour et désinstalle des applications Android pour AutoJs6 et ses scripts, avec installation silencieuse via Shizuku ou Root</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Setup Installer reprend l'installateur de paquets d'AutoJs6 : les boutons d'installation du gestionnaire de fichiers, du centre de plugins et du générateur de scripts empaquetés, l'entrée externe "Ouvrir avec" pour les fichiers `.apk`, `.apks`, `.xapk`, `.apkm` et `.apkz`, ainsi que l'objet global `installer` côté script pour installer, mettre à jour, inspecter et désinstaller des applications. En plus de la confirmation système habituelle, il peut installer et désinstaller silencieusement via Shizuku ou Root.

AutoJs6 découvre le plugin par son service Binder et lui transmet les fichiers de paquet sous forme de descripteurs de fichier en lecture seule ; le plugin analyse le paquet, choisit le mode d'autorisation, affiche au besoin sa propre boîte de dialogue de confirmation et de progression, puis renvoie les étapes, la progression et le résultat. Les opérations privilégiées s'exécutent dans un service utilisateur Shizuku ou un service root libsu qui dialogue directement avec l'installateur de paquets du système.

******

### État

******

1.0.0: Aperçu de développement P2: installation, inspection, requêtes utilisateur et désinstallation sont reliées au service hôte, avec confirmation explicite et nettoyage automatique des sessions. La validation complète des entrées hôtes, l'interface complète, l'ouverture externe, l'activation comme installateur par défaut, l'API de script et les paramètres restent en cours. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

******

### Fonctionnalités

******

Fonctionnalités prévues, livrées selon les phases de la feuille de route:

- Formats de paquet : `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` et archives ZIP contenant des APK ; les paquets fractionnés sont sélectionnés pour l'appareil ; les fichiers `.aab` sont reconnus et décrits mais pas installés.
- Modes d'autorisation : `none` (session PackageInstaller du système avec confirmation de l'utilisateur), `shizuku` et `root` ; `auto` choisit le premier disponible dans l'ordre configuré dans les paramètres, et un script peut en nommer un explicitement.
- Options d'installation : installation par lots, suppression du fichier source après succès, autorisation de la rétrogradation, autorisation des paquets de test, contournement du blocage des targetSdk bas (Android 14+), nom de paquet de l'installateur et utilisateur cible (modes privilégiés uniquement).
- Désinstallation silencieuse avec conservation facultative des données via Shizuku ou Root ; sinon la boîte de dialogue système habituelle.
- Définir comme installateur par défaut : avec Shizuku ou Root, le plugin devient le gestionnaire préféré des fichiers de paquet ; sans privilèges, la page système "Ouvrir par défaut" est ouverte pour vous.
- API de script `installer` (alias `$installer`) avec formes synchrone, `...Async` et session ; chaque échec est une `InstallerError` dotée d'un `code` stable.

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur un appareil doté d'AutoJs6 build 5299 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `3-Setup Installer` est reconnu et activez-le.
3. Touchez un fichier de paquet dans le gestionnaire de fichiers AutoJs6, ouvrez un paquet depuis n'importe quel gestionnaire de fichiers avec 3-Setup Installer, ou appelez `installer.install(...)` depuis un script. Pour une installation silencieuse, démarrez Shizuku ou accordez Root lorsque le plugin le demande, ou choisissez le mode d'autorisation dans les paramètres du plugin.

******

### Modes d'autorisation

******

Ce que chaque mode permet et ce dont il a besoin:

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku` : nécessite l'application Shizuku en cours d'exécution (démarrée via le débogage sans fil, ADB ou Root) et la permission accordée au plugin ; s'exécute avec les droits shell, qui permettent l'installation silencieuse, la désinstallation silencieuse, les autres utilisateurs et le verrouillage de l'installateur par défaut.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.
- **Remarque:** Quand les privilèges sont disponibles, l'API de script installe silencieusement par défaut et n'affiche aucun dialogue de confirmation de sa propre initiative. Si Android exige une confirmation, `interaction: 'auto'` autorise le dialogue système et l'indique dans `notes`. Utilisez explicitement `interaction: 'dialog'` pour confirmer avant l'installation, ou `interaction: 'silent'` pour échouer au lieu d'afficher une confirmation système.

******

### Demarrage rapide

******

Un script qui installe silencieusement, met à jour en autorisant la rétrogradation, observe une session et désinstalle (disponible à partir de la phase P4):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### Compatibilité

******

Faits de plateforme qui délimitent ce que le plugin peut faire:

- Android 7.0 (API 24) et plus récent ; le build hôte et le plugin sont vérifiés ensemble sur la matrice d'appareils listée dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- Le contournement du blocage des targetSdk bas existe depuis Android 14 (API 34) ; sur les systèmes plus anciens, l'option est ignorée et signalée dans le résultat.
- Certains systèmes OEM restreignent l'application pouvant être l'installateur par défaut ou exigent un nom de paquet d'installateur de confiance (HyperOS accepte `com.android.shell`) ; le plugin rapporte la réponse du système telle quelle.

******

### Questions fréquentes

******

- **Pourquoi l'installation demande-t-elle encore une confirmation ?** Le mode `none` passe toujours par la confirmation du système. Démarrez Shizuku ou accordez Root, puis choisissez ce mode dans les paramètres ou passez `authorizer: 'shizuku'` dans le script.
- **Peut-on installer un `.aab` ?** Non. Un Android App Bundle est un format de publication ; convertissez-le d'abord avec bundletool en un ensemble `.apks`. Le plugin reconnaît les fichiers `.aab` et affiche les informations de paquet et de modules.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul AutoJs6 peut donc les atteindre ; l'entrée externe "Ouvrir avec" n'accepte que des fichiers de paquet et n'exécute jamais de script.
- REQUEST_INSTALL_PACKAGES et REQUEST_DELETE_PACKAGES soutiennent les boîtes de dialogue habituelles d'installation et de désinstallation ; QUERY_ALL_PACKAGES permet au plugin d'afficher la version installée et de comparer les signatures avant une mise à jour.
- Shizuku et Root ne servent qu'à l'opération que vous lancez ; le service privilégié ne conserve aucun état, ne garde aucun shell ouvert entre les opérations et n'est jamais atteint depuis l'extérieur du plugin.
- Les fichiers de paquet sont ouverts en lecture seule ; le plugin n'effectue aucune requête réseau, ne collecte aucune donnée et exclut son stockage privé des sauvegardes.

N'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins ; l'hôte utilise ces identifiants pour découvrir le plugin et négocier la compatibilité:

```text
application id: io.github.supermonster003.autojs6.plugin.three.setup.installer
plugin id: three-setup-installer
engine: installer
variant: default
service action: org.autojs.plugin.INSTALLER
service category: installer
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.installer.api.IInstallerPlugin
minimum host build: 5299 (6.8.0)
```

`ThreeSetupInstallerPluginService` répond à `org.autojs.plugin.INSTALLER` (category `installer`) et implémente le contrat installer-api de l'hôte `org.autojs.plugin.installer.api.IInstallerPlugin` à partir de la phase P1. `ThreeSetupInstallerPluginInfoService` répond à `org.autojs.plugin.INFO` avec PluginInfo. `WakeActivity` permet à l'hôte d'activer le plugin.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/10/01_

- `Note` Aperçu de développement P2: installation, inspection, requêtes utilisateur et désinstallation sont reliées au service hôte, avec confirmation explicite et nettoyage automatique des sessions. La validation complète des entrées hôtes, l'interface complète, l'ouverture externe, l'activation comme installateur par défaut, l'API de script et les paramètres restent en cours.
- `Fonctionnalité` Identité du plugin `three-setup-installer` (engine `installer`) avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.INSTALLER` pour la découverte par l'hôte
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Amélioration` P0 a validé l'installation silencieuse, les mises à jour, la désinstallation et le choix ordinaire de l'installateur par défaut avec Shizuku et Root. Les points d'installation depuis l'hôte et les scripts ne sont pas encore disponibles; les valeurs par défaut persistantes restent hors de cette version.
- `Amélioration` L'identifiant du plugin, le moteur, l'action / la categorie du service, le descripteur Binder et la version minimale de l'hote proviennent desormais des constantes du contrat installer-api de l'hote; les capacites declarent la version 1 du contrat d'installation et la build minimale de l'hote est fixee a 5299
- `Amélioration` Les sources à accès aléatoire évitent une copie complète en cache, tandis que les flux sont stockés temporairement si nécessaire. Les paquets fractionnés en ZIP sont pris en charge, les AAB restent limités à l'inspection et les sources modifiées sont refusées.
- `Amélioration` Le mode d'autorisation choisi explicitement ne bascule jamais vers un autre. Le refus, le délai dépassé et l'incompatibilité sont distingués, et les requêtes simultanées partagent l'autorisation et les connexions privilégiées.
- `Amélioration` Le moteur d'installation et de mise à jour utilise la confirmation système, Shizuku ou Root, permet l'annulation et indique le mode de confirmation réel et la réponse du système.
- `Amélioration` Le moteur de désinstallation prend en charge la confirmation système, Shizuku et Root, avec conservation facultative des données en mode privilégié.
- `Amélioration` L'installation séquentielle par lot peut continuer après un échec ou annuler les éléments restants, avec validation et sélection des utilisateurs cibles en mode privilégié.
- `Amélioration` Le service hôte permet l'inspection, l'installation, la désinstallation et la consultation des utilisateurs, avec confirmation explicite, annulation à la fermeture de l'appelant, quatre sessions simultanées au maximum et nettoyage automatique.
- `Dépendance` Ajout de Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) pour le mode d'autorisation Shizuku
- `Dépendance` Ajout de libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) pour le mode d'autorisation Root
- `Dépendance` Ajout de AndroidHiddenApiBypass 6.1 pour les API cachées de l'installateur de paquets utilisées par le service privilégié
- `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5298, MPL 2.0) comme contrat de plugin partagé, avec hachage verrouillé dans `locks/host-api-aars.lock`
- `Dépendance` Ajout de `package-archive-parser.aar` et `installer-api.aar` (modules AutoJs6 `plugin-api/package-archive-parser` et `plugin-api/installer-api`, build hote P1 6.8.0 / 5299, MPL 2.0), avec hachage verrouille dans `locks/host-api-aars.lock` aux cotes de `common-plugin-api.aar`
- `Dépendance` Mise à jour de l'analyseur de paquets intégré pour reconnaître les conteneurs ZIP ordinaires avec APK fractionnés

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation et vérification

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources ; les utilisateurs ordinaires peuvent simplement installer l'APK préconstruit depuis la page Releases.

Compiler un APK de débogage:

```powershell
.\gradlew.bat :app:assembleDebug
```

Exécuter les tests unitaires JVM et compiler l'APK de tests d'instrumentation:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compiler l'APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collecter l'artefact de release et ajouter la version et le condensé CRC32 à son nom de fichier:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Vérifier que les sources de documentation multilingues et les artefacts générés sont synchronisés (également appliqué par la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilation nécessite JDK 21 ou ultérieur et Android SDK 37 ; les versions de Gradle et des plugins sont gérées de manière centralisée par `version.properties` et `io.github.supermonster003.autojs6-platform-versions`.

******

### Localisation et génération de la documentation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Les fichiers JSON de langue sous `.readme/` et `.changelog/` sont la source unique du README, des instructions du centre de plugins et du journal des modifications. Modifiez toujours ces sources JSON et relancez `py .python/generate_markdown.py` ; les artefacts README, `plugin_instruction.md` et journal des modifications générés ne sont jamais édités à la main. Exécutez `py .python/generate_markdown.py --check` pour vérifier tous les artefacts générés.

******

### Licence

******

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE). Les composants tiers et leurs licences sont listés dans les [Avis de tiers](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Documentation du module installer: https://docs.autojs6.com/#/installer
- InstallerX et InstallerX Revived (référence d'architecture, GPL-3.0, aucun code réutilisé): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
