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

3-Setup Installer installe, met à jour, inspecte et désinstalle des applications Android depuis son accueil autonome, les entrées et scripts AutoJs6, ou l'ouverture et le partage de paquets par d'autres applications. Il prend en charge la confirmation Android et les opérations privilégiées via Shizuku ou Root.

AutoJs6 découvre le plugin par son service Binder et lui transmet les fichiers de paquet sous forme de descripteurs de fichier en lecture seule ; le plugin analyse le paquet, choisit le mode d'autorisation, affiche au besoin sa propre boîte de dialogue de confirmation et de progression, puis renvoie les étapes, la progression et le résultat. Les opérations privilégiées s'exécutent dans un service utilisateur Shizuku ou un service root libsu qui dialogue directement avec l'installateur de paquets du système.

******

### État

******

1.0.0: Aperçu de développement avec accueil autonome, paramètres, gestion des applications installées, files séquentielles et historique d'installation. La confirmation, la progression, les résultats et les notifications au premier plan sont disponibles. Un redémarrage conserve les résultats confirmés enregistrés et marque les tâches inachevées comme annulées, sans reprise ni nouvelle tentative automatique. L'API de script `installer` nécessite AutoJs6 >= 6.8.0 (5300); l'intégration de base nécessite la version de compilation 5299. Voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour les appareils couverts et les validations restantes.

******

### Fonctionnalités

******

Fonctions disponibles dans cet aperçu de développement:

- Formats de paquet : `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` et archives ZIP contenant des APK ; les paquets fractionnés sont sélectionnés pour l'appareil ; les fichiers `.aab` sont reconnus et décrits mais pas installés.
- Autorisations: `none` utilise la confirmation Android; `shizuku` et `root` permettent les opérations privilégiées. Par défaut, `auto` choisit Shizuku, Root puis la confirmation système selon leur disponibilité. Les paramètres permettent de modifier l'ordre et d'activer les méthodes; un choix explicite ne bascule jamais silencieusement vers une autre méthode.
- La suppression de la source après une installation réussie peut être demandée sans garantie. Les rétrogradations, paquets de test, contournements du targetSdk minimal (Android 14+), attributions d'installateur et autres utilisateurs nécessitent Shizuku ou Root et restent soumis aux restrictions Android.
- La liste des applications installées permet une recherche par nom ou nom de paquet, un tri par nom, date d'installation ou de mise à jour, et l'affichage des applications système. Ouvrez une application ou sa fiche système, ou vérifiez puis confirmez sa désinstallation. Shizuku ou Root permettent ensuite de désinstaller sans nouvelle confirmation système, avec conservation facultative des données; les autres cas utilisent la confirmation Android.
- La confirmation affiche les informations de l'application, les versions ancienne et nouvelle, les signatures et les composants APK sélectionnables. La progression permet l'annulation; les résultats proposent les actions de réussite ou les erreurs à copier. Les installations par lots affichent chaque état séparément.
- Ouvrez ou partagez un ou plusieurs paquets, y compris les fichiers APKS partagés par MT Manager. Plusieurs paquets rejoignent une file séquentielle. Les éléments externes en échec peuvent être réessayés tant que leur URI et leur accès restent disponibles.
- Notifications de progression au premier plan, d'annulation et de résultats. Refuser la permission de notification n'empêche pas l'installation.
- L'apparence comprend la langue, le mode nuit, la couleur du thème et l'icône du lanceur. Les trois premiers suivent AutoJs6 par défaut et acceptent des choix locaux; sans hôte, la langue et le mode du système ainsi que la couleur par défaut s'appliquent. Les icônes proposent les modes clair, sombre, automatique et transparent; le mode automatique suit le système, selon le cache et les masques du lanceur.
- La carte d'accueil et les paramètres ouvrent la même page d'installateur par défaut, avec définition et suppression privilégiées ou indications vers les paramètres système sans privilèges. Les politiques OEM peuvent bloquer le changement ou imposer d'effacer l'ancien choix. Les scripts conservent `installer.isDefault`, `installer.setDefault` et `setDefaultAsync`; les résultats reflètent la réponse de l'appareil.
- L'API de scripts `installer` (alias `$installer`) propose les formes synchrone, `...Async` et session pour l'installation simple, par lots ou fractionnée, la désinstallation, l'inspection, les requêtes d'autorisation et d'utilisateurs, et l'installateur par défaut. Les erreurs sont des objets `InstallerError` avec un `code` stable (nécessite AutoJs6 >= 6.8.0 (5300)).
- L'accueil autonome affiche la disponibilité et les autorisations Shizuku/Root, l'installateur par défaut, les tâches actives et les installations récentes. Sélectionnez plusieurs paquets dans le sélecteur système pour les installer successivement, continuer après un échec individuel ou annuler les éléments restants.
- L'historique privé conserve au plus 200 éléments: paquet, nom, anciennes/nouvelles versions, résultat, date, origine (hôte/script/externe/accueil), autorisation et détails d'échec. Supprimez une entrée ou videz l'historique sans désinstaller les applications ni supprimer les sources. Après l'arrêt du processus, les éléments inachevés deviennent annulés sans reprise automatique.
- Les paramètres enregistrent l'ordre et l'activation des autorisations, les options d'installation et les notifications de progression. Les installations locales et externes utilisent `dialog` par défaut; les choix `auto` ou `silent` enregistrés explicitement s'appliquent. Les requêtes hôte/script conservent leurs options explicites, et l'API de script garde `auto` par défaut. Les choix ne sont enregistrés qu'après confirmation.
- Les paramètres donnent accès à la page À propos et à l'historique des versions intégré en dix langues. La recherche manuelle de mises à jour utilise l'API GitHub Releases du plugin avec un intervalle de 12 heures, un cache et la gestion des versions ignorées. Les pages de publication s'ouvrent dans le navigateur; aucune mise à jour n'est téléchargée ni installée automatiquement.

******

### Utilisation

******

1. Installez l'APK du plugin depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur Android 7.0 ou ultérieur. Son entrée dans le lanceur ouvre l'accueil autonome.
2. Sur l'accueil, vérifiez les autorisations et l'installateur par défaut, puis utilisez le bouton d'ajout pour sélectionner un ou plusieurs paquets. Vérifiez la boîte de dialogue avant de confirmer et suivez la progression et l'historique sur l'accueil.
3. Pour l'intégration AutoJs6, utilisez la version de compilation 5299 (6.8.0) ou ultérieure et activez `3-Setup Installer` dans le centre de plugins. L'API de script nécessite la version de compilation 5300 ou ultérieure.
4. Utilisez une action d'installation d'AutoJs6 ou choisissez 3-Setup Installer pour ouvrir ou partager des paquets. Si une confirmation apparaît, vérifiez l'application et les options avant d'installer. Préparez l'autorisation Shizuku ou Root si vous choisissez une méthode privilégiée.
5. Le menu d'accueil ouvre les applications installées et les paramètres. Vérifiez les réglages locaux d'installation, l'apparence, l'icône et les notifications; À propos, l'historique des versions et la recherche manuelle de mises à jour se trouvent dans les paramètres.

******

### Modes d'autorisation

******

Ce que chaque mode permet et ce dont il a besoin:

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku`: nécessite Shizuku en cours d'exécution (démarré par débogage sans fil, ADB ou Root) et la permission accordée au plugin. Les droits shell permettent l'installation et la désinstallation silencieuses, ainsi que les opérations pour d'autres utilisateurs.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.
- **Remarque:** Lorsque les privilèges sont disponibles, les demandes de l'hôte avec `interaction: 'auto'` installent silencieusement sans ouvrir d'abord une confirmation. Si Android exige une confirmation, `auto` l'autorise et la consigne dans `notes`. Utilisez `interaction: 'dialog'` pour confirmer avant l'installation, ou `interaction: 'silent'` pour échouer si une confirmation système est nécessaire. L'API de scripts suit le même comportement par défaut.
- Les paramètres enregistrent l'ordre et l'activation des autorisations, les options d'installation et les notifications de progression. Les installations locales et externes utilisent `dialog` par défaut; les choix `auto` ou `silent` enregistrés explicitement s'appliquent. Les requêtes hôte/script conservent leurs options explicites, et l'API de script garde `auto` par défaut. Les choix ne sont enregistrés qu'après confirmation.

******

### Demarrage rapide

******

Fonctions modèles pour l'installation, les lots et les sessions (nécessite AutoJs6 >= 6.8.0 (5300)). Choisissez et vérifiez les sources avant d'appeler une fonction. Cet exemple n'installe ni ne désinstalle automatiquement et ne modifie pas l'installateur par défaut.:

```js
// Read-only probe. The functions below run only when explicitly called with chosen sources.
console.log(installer.status);

// An already authorized Shizuku service is required; silent never falls back to a dialog.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// An array means independent applications, including an array containing one source.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// A source may also be { splits: [...] } for one application's split files.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};
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
- **Pourquoi la source n'a-t-elle pas été supprimée?** La suppression n'est tentée qu'après une installation réussie; son échec ne change pas ce résultat. Le fournisseur externe peut refuser la suppression. Pour les scripts, l'hôte applique `deleteSource` aux chemins et sources `file://`, en conservant les sources `content://` et les éléments en échec. Consultez `sourceDeleted` et `notes`.
- **Puis-je réessayer ou reprendre?** Un URI externe en échec peut être réessayé tant que la source et son accès sont disponibles. Si la source ou l'accès a été libéré, rouvrez le paquet. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative. Vérifiez l'application installée avant de recommencer.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul AutoJs6 peut donc les atteindre ; l'entrée externe "Ouvrir avec" n'accepte que des fichiers de paquet et n'exécute jamais de script.
- REQUEST_INSTALL_PACKAGES et REQUEST_DELETE_PACKAGES permettent la confirmation Android. QUERY_ALL_PACKAGES sert à gérer les applications installées, comparer versions et signatures, et détecter l'installateur par défaut.
- FOREGROUND_SERVICE et FOREGROUND_SERVICE_DATA_SYNC soutiennent l'installation en arrière-plan; POST_NOTIFICATIONS permet les notifications de progression et de résultats. L'absence de permission de notification ne bloque pas l'installation.
- Shizuku et Root ne servent qu'à l'opération que vous lancez ; le service privilégié ne conserve aucun état, ne garde aucun shell ouvert entre les opérations et n'est jamais atteint depuis l'extérieur du plugin.
- L'installation, l'inspection, l'historique et la gestion des applications fonctionnent hors ligne. INTERNET est utilisé uniquement lors d'une recherche manuelle de versions auprès de l'API GitHub Releases fixe du plugin, avec un intervalle de 12 heures. Aucune recherche en arrière-plan ni aucun envoi de paquets n'est effectué.
- Les sources sont ouvertes en lecture seule. L'historique conserve des métadonnées limitées et des résultats, sans contenu de paquet ni URI source; les chemins des erreurs sont masqués. Le stockage privé est exclu des sauvegardes. Supprimer une entrée ne désinstalle pas l'application et ne supprime pas sa source.

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

- `Note` Aperçu de développement avec accueil autonome, paramètres, gestion des applications installées, files séquentielles et historique d'installation. La confirmation, la progression, les résultats et les notifications au premier plan sont disponibles. Un redémarrage conserve les résultats confirmés enregistrés et marque les tâches inachevées comme annulées, sans reprise ni nouvelle tentative automatique. L'API de script `installer` nécessite AutoJs6 >= 6.8.0 (5300); l'intégration de base nécessite la version de compilation 5299. Voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour les appareils couverts et les validations restantes
- `Fonctionnalité` 3-Setup Installer installe, met à jour, inspecte et désinstalle des applications Android depuis son accueil autonome, les entrées et scripts AutoJs6, ou l'ouverture et le partage de paquets par d'autres applications. Il prend en charge la confirmation Android et les opérations privilégiées via Shizuku ou Root
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Fonctionnalité` L'API de scripts `installer` (alias `$installer`) propose les formes synchrone, `...Async` et session pour l'installation simple, par lots ou fractionnée, la désinstallation, l'inspection, les requêtes d'autorisation et d'utilisateurs, et l'installateur par défaut. Les erreurs sont des objets `InstallerError` avec un `code` stable (nécessite AutoJs6 >= 6.8.0 (5300))
- `Fonctionnalité` L'accueil autonome affiche la disponibilité et les autorisations Shizuku/Root, l'installateur par défaut, les tâches actives et les installations récentes. Sélectionnez plusieurs paquets dans le sélecteur système pour les installer successivement, continuer après un échec individuel ou annuler les éléments restants
- `Fonctionnalité` L'historique privé conserve au plus 200 éléments: paquet, nom, anciennes/nouvelles versions, résultat, date, origine (hôte/script/externe/accueil), autorisation et détails d'échec. Supprimez une entrée ou videz l'historique sans désinstaller les applications ni supprimer les sources. Après l'arrêt du processus, les éléments inachevés deviennent annulés sans reprise automatique
- `Fonctionnalité` La liste des applications installées permet une recherche par nom ou nom de paquet, un tri par nom, date d'installation ou de mise à jour, et l'affichage des applications système. Ouvrez une application ou sa fiche système, ou vérifiez puis confirmez sa désinstallation. Shizuku ou Root permettent ensuite de désinstaller sans nouvelle confirmation système, avec conservation facultative des données; les autres cas utilisent la confirmation Android
- `Fonctionnalité` Les paramètres enregistrent l'ordre et l'activation des autorisations, les options d'installation et les notifications de progression. Les installations locales et externes utilisent `dialog` par défaut; les choix `auto` ou `silent` enregistrés explicitement s'appliquent. Les requêtes hôte/script conservent leurs options explicites, et l'API de script garde `auto` par défaut. Les choix ne sont enregistrés qu'après confirmation
- `Fonctionnalité` La carte d'accueil et les paramètres ouvrent la même page d'installateur par défaut, avec définition et suppression privilégiées ou indications vers les paramètres système sans privilèges. Les politiques OEM peuvent bloquer le changement ou imposer d'effacer l'ancien choix. Les scripts conservent `installer.isDefault`, `installer.setDefault` et `setDefaultAsync`; les résultats reflètent la réponse de l'appareil
- `Fonctionnalité` Les paramètres donnent accès à la page À propos et à l'historique des versions intégré en dix langues. La recherche manuelle de mises à jour utilise l'API GitHub Releases du plugin avec un intervalle de 12 heures, un cache et la gestion des versions ignorées. Les pages de publication s'ouvrent dans le navigateur; aucune mise à jour n'est téléchargée ni installée automatiquement
- `Correctif` Les actions d'annulation ne suivaient pas la langue du plugin sur les appareils dépourvus de la traduction système correspondante
- `Correctif` Correction de la coche invisible des APK fractionnés obligatoires tels que base.apk lorsque leur case est désactivée, dans les thèmes clair et sombre
- `Correctif` Correction de l'absence du plugin dans le sélecteur pour les conteneurs de paquets ouverts depuis Files by Google ou un fournisseur utilisant une URI opaque et un type MIME ZIP ou binaire générique
- `Correctif` Refus des fournisseurs de paquets renvoyant des descripteurs modifiables et fermeture immédiate des descripteurs rejetés
- `Amélioration` L'identifiant du plugin, le moteur, l'action / la categorie du service, le descripteur Binder et la version minimale de l'hote proviennent desormais des constantes du contrat installer-api de l'hote; les capacites declarent la version 1 du contrat d'installation et la build minimale de l'hote est fixee a 5299
- `Amélioration` Les sources à accès aléatoire évitent une copie complète en cache, tandis que les flux sont stockés temporairement si nécessaire. Les paquets fractionnés en ZIP sont pris en charge, les AAB restent limités à l'inspection et les sources modifiées sont refusées.
- `Amélioration` Le mode d'autorisation choisi explicitement ne bascule jamais vers un autre. Le refus, le délai dépassé et l'incompatibilité sont distingués, et les requêtes simultanées partagent l'autorisation et les connexions privilégiées.
- `Amélioration` Le moteur d'installation et de mise à jour utilise la confirmation système, Shizuku ou Root, permet l'annulation et indique le mode de confirmation réel et la réponse du système.
- `Amélioration` Le moteur de désinstallation prend en charge la confirmation système, Shizuku et Root, avec conservation facultative des données en mode privilégié.
- `Amélioration` L'installation séquentielle par lot peut continuer après un échec ou annuler les éléments restants, avec validation et sélection des utilisateurs cibles en mode privilégié.
- `Amélioration` Le service hôte permet l'inspection, l'installation, la désinstallation et la consultation des utilisateurs, avec confirmation explicite, annulation à la fermeture de l'appelant, quatre sessions simultanées au maximum et nettoyage automatique.
- `Amélioration` L'apparence comprend la langue, le mode nuit, la couleur du thème et l'icône du lanceur. Les trois premiers suivent AutoJs6 par défaut et acceptent des choix locaux; sans hôte, la langue et le mode du système ainsi que la couleur par défaut s'appliquent. Les icônes proposent les modes clair, sombre, automatique et transparent; le mode automatique suit le système, selon le cache et les masques du lanceur
- `Amélioration` L'installation en arrière-plan dispose d'un service de premier plan et de notifications de progression, annulation et résultats. Refuser les notifications ne bloque pas l'installation.
- `Amélioration` Ajout de dialogues de confirmation, progression et résultats avec informations, sélection des composants APK, options, copie des erreurs et état par élément. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative.
- `Amélioration` La confirmation système gère les indications de permission des sources inconnues et les interruptions. La désinstallation privilégiée affiche les informations et le choix de conserver les données avant confirmation.
- `Amélioration` Ouvrez ou partagez un ou plusieurs paquets, réessayez les sources externes encore accessibles et demandez leur suppression après succès. Un refus de suppression préserve la réussite de l'installation.
- `Amélioration` L'ouverture des paquets APKS partagés par MT Manager prend en charge le type MIME application/vnd.android.package-archives
- `Dépendance` Ajout de Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) pour le mode d'autorisation Shizuku
- `Dépendance` Ajout de libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) pour le mode d'autorisation Root
- `Dépendance` Ajout de AndroidHiddenApiBypass 6.1 pour les API cachées de l'installateur de paquets utilisées par le service privilégié
- `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5298, MPL 2.0) comme contrat de plugin partagé, avec hachage verrouillé dans `locks/host-api-aars.lock`
- `Dépendance` Ajout de `package-archive-parser.aar` et `installer-api.aar` (modules AutoJs6 `plugin-api/package-archive-parser` et `plugin-api/installer-api`, MPL 2.0), avec hachage verrouille dans `locks/host-api-aars.lock` aux cotes de `common-plugin-api.aar`
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
