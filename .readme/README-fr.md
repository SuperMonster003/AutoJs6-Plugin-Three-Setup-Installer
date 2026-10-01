<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Installe, met à jour et désinstalle les applications Android avec confirmation système, Shizuku ou Root</p>

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

3-Setup Installer installe, met à jour, inspecte et désinstalle les applications Android via les fonctions d'installation d'AutoJs6 et les demandes externes d'ouverture ou de partage de paquets. Il prend en charge la confirmation Android habituelle et l'installation privilégiée avec Shizuku ou Root. L'API de scripts et les pages autonomes d'accueil et de paramètres restent prévues.

AutoJs6 découvre le plugin par son service Binder et lui transmet les fichiers de paquet sous forme de descripteurs de fichier en lecture seule ; le plugin analyse le paquet, choisit le mode d'autorisation, affiche au besoin sa propre boîte de dialogue de confirmation et de progression, puis renvoie les étapes, la progression et le résultat. Les opérations privilégiées s'exécutent dans un service utilisateur Shizuku ou un service root libsu qui dialogue directement avec l'installateur de paquets du système.

******

### État

******

1.0.0: Aperçu de développement P3. Les dialogues de confirmation, progression, résultats et lots, l'ouverture et le partage externes, la suppression facultative de la source, la confirmation système et les notifications de premier plan sont implémentés. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative. L'API de scripts, l'accueil et les paramètres autonomes, l'historique et la configuration de l'installateur par défaut restent prévus. Consultez [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour la progression et les appareils couverts. AutoJs6 >= 6.8.0 (5299).

******

### Fonctionnalités

******

Fonctions disponibles dans cet aperçu de développement; les fonctions futures sont indiquées explicitement:

- Formats de paquet : `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` et archives ZIP contenant des APK ; les paquets fractionnés sont sélectionnés pour l'appareil ; les fichiers `.aab` sont reconnus et décrits mais pas installés.
- Méthodes d'autorisation: `none` utilise la confirmation Android; `shizuku` et `root` offrent les opérations privilégiées. `auto` choisit Shizuku, puis Root, puis la confirmation système selon leur disponibilité. Le dialogue d'installation permet de choisir la méthode.
- La suppression de la source après une installation réussie peut être demandée sans garantie. Les rétrogradations, paquets de test, contournements du targetSdk minimal (Android 14+), attributions d'installateur et autres utilisateurs nécessitent Shizuku ou Root et restent soumis aux restrictions Android.
- Désinstallation silencieuse avec conservation facultative des données via Shizuku ou Root ; sinon la boîte de dialogue système habituelle.
- La confirmation affiche les informations de l'application, les versions ancienne et nouvelle, les signatures et les composants APK sélectionnables. La progression permet l'annulation; les résultats proposent les actions de réussite ou les erreurs à copier. Les installations par lots affichent chaque état séparément.
- Ouvrez des paquets ou partagez un ou plusieurs fichiers avec le plugin. Les sources externes en échec peuvent être réessayées tant que leur URI et leur accès restent disponibles.
- Notifications de progression au premier plan, d'annulation et de résultats. Refuser la permission de notification n'empêche pas l'installation.
- Les dialogues suivent par défaut la langue, le mode nuit et la couleur d'AutoJs6. Si l'hôte est indisponible, ils utilisent la langue et le mode nuit du système ainsi qu'une couleur par défaut.
- Prévu: configuration de l'installateur par défaut, avec sélection privilégiée et aide vers les paramètres système si nécessaire.
- Prévu pour P4: API de scripts `installer` (alias `$installer`) sous formes synchrone, `...Async` et session, et erreurs `InstallerError` avec des valeurs `code` stables.
- Prévu pour P5: accueil et paramètres autonomes, historique des installations et gestion des applications installées.

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur un appareil doté d'AutoJs6 build 5299 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `3-Setup Installer` est reconnu et activez-le.
3. Utilisez une action d'installation d'AutoJs6 ou choisissez 3-Setup Installer pour ouvrir ou partager des paquets. Si une confirmation apparaît, vérifiez l'application et les options avant d'installer. Préparez l'autorisation Shizuku ou Root si vous choisissez une méthode privilégiée.

******

### Modes d'autorisation

******

Ce que chaque mode permet et ce dont il a besoin:

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku`: nécessite Shizuku en cours d'exécution (démarré par débogage sans fil, ADB ou Root) et la permission accordée au plugin. Les droits shell permettent l'installation et la désinstallation silencieuses, ainsi que les opérations pour d'autres utilisateurs.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.
- **Remarque:** Lorsque les privilèges sont disponibles, les demandes de l'hôte avec `interaction: 'auto'` installent silencieusement sans ouvrir d'abord une confirmation. Si Android exige une confirmation, `auto` l'autorise et la consigne dans `notes`. Utilisez `interaction: 'dialog'` pour confirmer avant l'installation, ou `interaction: 'silent'` pour échouer si une confirmation système est nécessaire. L'API de scripts prévue suivra le même comportement par défaut.

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

- Android 7.0 (API 24) et versions ultérieures. La validation des appareils et la couverture restante sont consignées dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- Le contournement du blocage des targetSdk bas existe depuis Android 14 (API 34) ; sur les systèmes plus anciens, l'option est ignorée et signalée dans le résultat.
- Certains systèmes OEM restreignent l'application pouvant être l'installateur par défaut ou exigent un nom de paquet d'installateur de confiance (HyperOS accepte `com.android.shell`) ; le plugin rapporte la réponse du système telle quelle.

******

### Questions fréquentes

******

- **Pourquoi une confirmation est-elle encore nécessaire?** `none` utilise toujours la confirmation système. Préparez l'autorisation puis choisissez Shizuku ou Root dans le dialogue d'installation. Android ou la politique de l'appareil peut encore exiger une confirmation.
- **Peut-on installer un `.aab` ?** Non. Un Android App Bundle est un format de publication ; convertissez-le d'abord avec bundletool en un ensemble `.apks`. Le plugin reconnaît les fichiers `.aab` et affiche les informations de paquet et de modules.
- **Pourquoi la source n'a-t-elle pas été supprimée?** La suppression est tentée uniquement après une installation réussie et peut être refusée par le fournisseur. L'installation reste réussie. Si AutoJs6 ou une autre application expéditrice possède la source, cette application est responsable de sa suppression.
- **Puis-je réessayer ou reprendre?** Un URI externe en échec peut être réessayé tant que la source et son accès sont disponibles. Si la source ou l'accès a été libéré, rouvrez le paquet. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative. Vérifiez l'application installée avant de recommencer.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul AutoJs6 peut donc les atteindre ; l'entrée externe "Ouvrir avec" n'accepte que des fichiers de paquet et n'exécute jamais de script.
- REQUEST_INSTALL_PACKAGES et REQUEST_DELETE_PACKAGES soutiennent les boîtes de dialogue habituelles d'installation et de désinstallation ; QUERY_ALL_PACKAGES permet au plugin d'afficher la version installée et de comparer les signatures avant une mise à jour.
- FOREGROUND_SERVICE et FOREGROUND_SERVICE_DATA_SYNC soutiennent l'installation en arrière-plan; POST_NOTIFICATIONS permet les notifications de progression et de résultats. L'absence de permission de notification ne bloque pas l'installation.
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

- `Note` Aperçu de développement P3. Les dialogues de confirmation, progression, résultats et lots, l'ouverture et le partage externes, la suppression facultative de la source, la confirmation système et les notifications de premier plan sont implémentés. L'API de scripts, l'accueil et les paramètres autonomes, l'historique et la configuration de l'installateur par défaut restent prévus. Consultez [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour la progression et les appareils couverts. AutoJs6 >= 6.8.0 (5299).
- `Fonctionnalité` Identité du plugin `three-setup-installer` (engine `installer`) avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.INSTALLER` pour la découverte par l'hôte
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Correctif` Les actions d'annulation ne suivaient pas la langue du plugin sur les appareils dépourvus de la traduction système correspondante
- `Amélioration` L'identifiant du plugin, le moteur, l'action / la categorie du service, le descripteur Binder et la version minimale de l'hote proviennent desormais des constantes du contrat installer-api de l'hote; les capacites declarent la version 1 du contrat d'installation et la build minimale de l'hote est fixee a 5299
- `Amélioration` Les sources à accès aléatoire évitent une copie complète en cache, tandis que les flux sont stockés temporairement si nécessaire. Les paquets fractionnés en ZIP sont pris en charge, les AAB restent limités à l'inspection et les sources modifiées sont refusées.
- `Amélioration` Le mode d'autorisation choisi explicitement ne bascule jamais vers un autre. Le refus, le délai dépassé et l'incompatibilité sont distingués, et les requêtes simultanées partagent l'autorisation et les connexions privilégiées.
- `Amélioration` Le moteur d'installation et de mise à jour utilise la confirmation système, Shizuku ou Root, permet l'annulation et indique le mode de confirmation réel et la réponse du système.
- `Amélioration` Le moteur de désinstallation prend en charge la confirmation système, Shizuku et Root, avec conservation facultative des données en mode privilégié.
- `Amélioration` L'installation séquentielle par lot peut continuer après un échec ou annuler les éléments restants, avec validation et sélection des utilisateurs cibles en mode privilégié.
- `Amélioration` Le service hôte permet l'inspection, l'installation, la désinstallation et la consultation des utilisateurs, avec confirmation explicite, annulation à la fermeture de l'appelant, quatre sessions simultanées au maximum et nettoyage automatique.
- `Amélioration` Les dialogues suivent la langue, le mode nuit et la couleur d'AutoJs6, avec un repli sans hôte et une présentation adaptée au texte agrandi et au RTL.
- `Amélioration` L'installation en arrière-plan dispose d'un service de premier plan et de notifications de progression, annulation et résultats. Refuser les notifications ne bloque pas l'installation.
- `Amélioration` Ajout de dialogues de confirmation, progression et résultats avec informations, sélection des composants APK, options, copie des erreurs et état par élément. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative.
- `Amélioration` La confirmation système gère les indications de permission des sources inconnues et les interruptions. La désinstallation privilégiée affiche les informations et le choix de conserver les données avant confirmation.
- `Amélioration` Ouvrez ou partagez un ou plusieurs paquets, réessayez les sources externes encore accessibles et demandez leur suppression après succès. Un refus de suppression préserve la réussite de l'installation.
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
