<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Installe, met à jour et désinstalle les applications avec confirmation Android, Shizuku, Root ou Dhizuku</p>

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

3-Setup Installer installe, met à jour, inspecte et désinstalle des applications Android depuis son accueil autonome, les entrées et scripts AutoJs6, ou l'ouverture et le partage de paquets par d'autres applications. Il prend en charge la confirmation Android et les opérations privilégiées via Shizuku, Root ou Dhizuku.

Le plugin gère l'inspection des paquets, l'installation et les résultats indépendamment de l'hôte. Choisissez le dialogue, le mode silencieux ou les notifications. En mode notification, la confirmation, l'annulation et les résultats restent dans les notifications; la confirmation Android ne s'ouvre qu'après un appui sur sa notification.

******

### État

******

1.1.0 décrit les fonctions d'installation, de gestion des applications et de scripts présentées ci-dessous. La publication officielle sur GitHub Releases et le référencement dans le centre de plugins restent à effectuer. L'intégration nécessite AutoJs6 >= 6.8.0 (5299), et l'API de scripts `installer` nécessite la compilation 5300 ou ultérieure. La couverture des appareils et les validations restantes figurent dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). Dhizuku, l'installation par notification et les options de scripts pour l'installateur persistant nécessitent AutoJs6 6.8.0 compilation 5307 ou ultérieure avec le contrat installer V2. L'intégration de base reste disponible dès la compilation 5299, et les méthodes de scripts V1 dès 5300.

******

### Fonctionnalités

******

Fonctions implémentées dans 1.1.0:

- Formats de paquet : `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` et archives ZIP contenant des APK ; les paquets fractionnés sont sélectionnés pour l'appareil ; les fichiers `.aab` sont reconnus et décrits mais pas installés.
- `none` utilise la confirmation Android. Les nouveaux paramètres essaient `shizuku -> root -> dhizuku -> none` pour `auto`, selon leur disponibilité; chaque méthode peut être déplacée ou désactivée. Les anciens paramètres à trois méthodes conservent leur ordre relatif et leurs activations, avec Dhizuku inséré avant `none` mais désactivé. Un choix explicite ne bascule jamais vers une autre méthode.
- La suppression de la source après une installation réussie peut être demandée sans garantie. Les rétrogradations, paquets de test, contournements du targetSdk minimal (Android 14+), attributions d'installateur et autres utilisateurs nécessitent Shizuku ou Root et restent soumis aux restrictions Android.
- Recherchez les applications par nom ou paquet, triez par nom ou dates d'installation et de mise à jour, et affichez les applications système si nécessaire. Ouvrez une application, sa fiche système ou confirmez sa désinstallation. Shizuku et Root permettent la suppression silencieuse avec conservation facultative des données; Dhizuku permet la suppression privilégiée uniquement pour l'utilisateur propriétaire courant, sans `keepData`.
- La confirmation affiche les informations de l'application, les versions ancienne et nouvelle, les signatures et les composants APK sélectionnables. La progression permet l'annulation; les résultats proposent les actions de réussite ou les erreurs à copier. Les installations par lots affichent chaque état séparément.
- Ouvrez ou partagez un ou plusieurs paquets, y compris les fichiers APKS partagés par MT Manager. Plusieurs paquets rejoignent une file séquentielle. Les éléments externes en échec peuvent être réessayés tant que leur URI et leur accès restent disponibles.
- `interaction: 'notification'` concerne uniquement l'installation: les notifications présentent confirmation initiale, annulation, progression et résultats sans dialogues d'installation du plugin. La confirmation Android exige toujours un appui sur sa notification. La permission, les notifications de l'application et le canal d'installation doivent être activés; sinon la requête échoue avec `NOTIFICATION_UNAVAILABLE`. Les autres modes tolèrent l'absence de permission de notification. La désinstallation n'accepte pas `notification`.
- L'apparence comprend la langue, le mode nuit, la couleur du thème et l'icône du lanceur. Les trois premiers suivent AutoJs6 par défaut et acceptent des choix locaux; sans hôte, la langue et le mode du système ainsi que la couleur par défaut s'appliquent. Les icônes proposent les modes clair, sombre, automatique et transparent; le mode automatique suit le système, selon le cache et les masques du lanceur.
- La page de l'installateur par défaut distingue préférence ordinaire et règle persistante. La préférence utilise Shizuku ou Root et reste soumise à la ROM. Dhizuku gère les règles persistantes sur API 26-33; API 34+ est refusé avant toute modification car la réponse du propriétaire ne peut pas être vérifiée. Root utilise un auxiliaire sous UID système uniquement pour l'utilisateur 0 sur les appareils compatibles. Les règles persistantes concurrentes ne sont pas remplacées. `persistentConfigured` atteste une ancienne configuration réussie, sans prouver la règle système actuelle; l'observation passive indique seulement `preferred` ou `none`.
- L'API de scripts `installer` (alias `$installer`) propose les formes synchrone, `...Async` et session pour l'installation simple, par lots ou fractionnée, la désinstallation, l'inspection, les requêtes d'autorisation et d'utilisateurs, et l'installateur par défaut. Les erreurs sont des objets `InstallerError` avec un `code` stable (nécessite AutoJs6 >= 6.8.0 (5300)).
- L'accueil autonome affiche la disponibilité et les autorisations Shizuku/Root/Dhizuku, l'installateur par défaut, les tâches actives et les installations récentes. Sélectionnez plusieurs paquets dans le sélecteur système pour les installer successivement, continuer après un échec individuel ou annuler les éléments restants.
- L'historique privé conserve au plus 200 éléments: paquet, nom, anciennes/nouvelles versions, résultat, date, origine (hôte/script/externe/accueil), autorisation et détails d'échec. Supprimez une entrée ou videz l'historique sans désinstaller les applications ni supprimer les sources. Après l'arrêt du processus, les éléments inachevés deviennent annulés sans reprise automatique.
- Les paramètres enregistrent l'ordre et l'activation des méthodes, les options d'installation et les préférences de notification. Les installations locales/externes utilisent `dialog` par défaut; `auto`, `silent` ou `notification` peuvent être choisis explicitement. L'interface hôte utilise `dialog`; les scripts gardent leurs options explicites et `auto` par défaut. Les changements sont enregistrés après confirmation.
- Les paramètres donnent accès à la page À propos et à l'historique des versions intégré en dix langues. La recherche manuelle de mises à jour utilise l'API GitHub Releases du plugin avec un intervalle de 12 heures, un cache et la gestion des versions ignorées. Les pages de publication s'ouvrent dans le navigateur; aucune mise à jour n'est téléchargée ni installée automatiquement.
- `dhizuku`: nécessite Android 8.0 (API 26)+, un propriétaire d'appareil/profil Dhizuku actif et l'autorisation donnée au plugin. Les opérations sont limitées à l'utilisateur du propriétaire courant; l'attribution de l'installation utilise le vrai paquet propriétaire. Les options shell/root de rétrogradation, paquet de test, contournement du targetSdk, autres utilisateurs, attribution arbitraire et conservation des données à la désinstallation sont refusées. Le plugin ne configure pas de propriétaire.

******

### Utilisation

******

1. Sur Android 7.0 ou ultérieur, installez l'APK officiel depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) après sa publication, ou utilisez l'assistant du centre de plugins AutoJs6 après son ajout à l'index officiel. Avant publication, testez une compilation fournie par le mainteneur ou compilée depuis les sources. L'icône du lanceur ouvre l'accueil autonome.
2. Sur l'accueil, vérifiez l'autorisation et l'installateur par défaut, puis sélectionnez un ou plusieurs paquets. Confirmez dans le dialogue ou la notification choisi et suivez les tâches et l'historique.
3. Pour l'intégration AutoJs6, utilisez la version de compilation 5299 (6.8.0) ou ultérieure et activez `3-Setup Installer` dans le centre de plugins. L'API de script nécessite la version de compilation 5300 ou ultérieure. Dhizuku, l'installation par notification et les options de scripts pour l'installateur persistant nécessitent AutoJs6 6.8.0 compilation 5307 ou ultérieure avec le contrat installer V2. L'intégration de base reste disponible dès la compilation 5299, et les méthodes de scripts V1 dès 5300.
4. Utilisez une action d'installation d'AutoJs6 ou choisissez 3-Setup Installer pour ouvrir ou partager des paquets. Si une confirmation apparaît, vérifiez l'application et les options avant d'installer. Préparez l'autorisation Shizuku, Root ou Dhizuku si vous choisissez une méthode privilégiée.
5. Le menu d'accueil ouvre les applications installées et les paramètres. Vérifiez les réglages locaux d'installation, l'apparence, l'icône et les notifications; À propos, l'historique des versions et la recherche manuelle de mises à jour se trouvent dans les paramètres.

******

### Modes d'autorisation

******

Ce que chaque mode permet et ce dont il a besoin:

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku`: nécessite Shizuku en cours d'exécution (lancé par débogage sans fil, ADB ou Root) et une autorisation propre à 3-Setup Installer. L'autorisation donnée à AutoJs6 ne couvre pas ce plugin. Les installations, désinstallations et opérations pour d'autres utilisateurs utilisent les privilèges du service Shizuku actif.
- `root`: nécessite un appareil rooté et un gestionnaire Root accordant `su` à 3-Setup Installer. Il permet l'installation, la désinstallation et la gestion des utilisateurs et de l'installateur par défaut via libsu. Android et la ROM décident toujours si chaque opération est autorisée.
- `dhizuku`: nécessite Android 8.0 (API 26)+, un propriétaire d'appareil/profil Dhizuku actif et l'autorisation donnée au plugin. Les opérations sont limitées à l'utilisateur du propriétaire courant; l'attribution de l'installation utilise le vrai paquet propriétaire. Les options shell/root de rétrogradation, paquet de test, contournement du targetSdk, autres utilisateurs, attribution arbitraire et conservation des données à la désinstallation sont refusées. Le plugin ne configure pas de propriétaire.
- **Remarque:** Les scripts utilisent `interaction: 'auto'` par défaut et installent silencieusement si les privilèges sont disponibles. Les actions d'installation de l'interface hôte utilisent `dialog`. Si Android exige une confirmation, `auto` l'autorise et la consigne dans `notes`. Choisissez `interaction: 'dialog'` pour confirmer avant l'installation. Le choix explicite `silent` échoue avec `AUTHORIZER_REQUIRED` si les privilèges manquent ou si une confirmation système est nécessaire.
- Les paramètres enregistrent l'ordre et l'activation des méthodes, les options d'installation et les préférences de notification. Les installations locales/externes utilisent `dialog` par défaut; `auto`, `silent` ou `notification` peuvent être choisis explicitement. L'interface hôte utilise `dialog`; les scripts gardent leurs options explicites et `auto` par défaut. Les changements sont enregistrés après confirmation.

******

### Démarrage rapide

******

Exemples de `install`, `installAsync`, `session`, `uninstall` et `setDefault` pour AutoJs6 >= 6.8.0 (5300). Les fonctions ne s'exécutent que si vous les appelez avec les sources, le nom de paquet ou le choix d'installateur sélectionnés. La requête d'état initiale est en lecture seule.

```js
// Informations de disponibilité et de compatibilité en lecture seule.
console.log(installer.status);

// Autorisation Shizuku requise; silent échoue si Android exige une confirmation.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// Le tableau contient des paquets indépendants, chacun avec son résultat.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// Toutes les parties appartiennent à une application, avec son APK de base.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// Démarre à l'appel; conservez la session pour annuler ou attendre.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// Choisissez le paquet à supprimer; keepData demande de conserver les données.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true définit ce plugin par défaut; false efface son choix. Limites de la ROM.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// Exemples V2: compilation hôte 5307 requise. Dhizuku exige un propriétaire actif; les notifications doivent être autorisées.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root persistant exige un accès UID système compatible pour l'utilisateur 0; les règles concurrentes sont conservées.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});
```

Une source peut être un chemin, une URI `file://` ou une URI `content://` lisible. Un tableau représente des éléments indépendants; `{ splits: [...] }` installe une seule application. `session(...)` démarre immédiatement; l'objet retourné propose `cancel()` et `wait()`. Les appels synchrones peuvent lever `InstallerError` et sont interdits sur le thread UI. Cela concerne aussi la lecture de `installer.status`, la création de `installer.session(...)` et l'appel de `session.wait()`. Utilisez les méthodes Async sur le thread UI, ou exécutez les opérations synchrones sur un thread de travail du script. Un objet session doit être utilisé uniquement sur le thread du script qui l'a créé. Gérez les rejets des Promise et vérifiez `ok` et `error` pour chaque résultat du lot. Un plugin absent ou incompatible produit `PLUGIN_UNAVAILABLE`. `setDefault` indique si l'état demandé a été atteint, y compris l'effacement du choix par défaut. `app.uninstall` reste le raccourci système de l'hôte; utilisez `installer.uninstall` pour les options privilégiées. Consultez la [documentation API installer](https://docs.autojs6.com/#/installer) pour les options et événements.

Dhizuku, l'installation par notification et les options de scripts pour l'installateur persistant nécessitent AutoJs6 6.8.0 compilation 5307 ou ultérieure avec le contrat installer V2. L'intégration de base reste disponible dès la compilation 5299, et les méthodes de scripts V1 dès 5300.

******

### Compatibilité

******

Faits de plateforme qui délimitent ce que le plugin peut faire:

- Android 7.0 (API 24) et versions ultérieures. La validation des appareils et la couverture restante sont consignées dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- Le contournement du blocage des targetSdk bas existe depuis Android 14 (API 34) ; sur les systèmes plus anciens, l'option est ignorée et signalée dans le résultat.
- La page de l'installateur par défaut distingue préférence ordinaire et règle persistante. La préférence utilise Shizuku ou Root et reste soumise à la ROM. Dhizuku gère les règles persistantes sur API 26-33; API 34+ est refusé avant toute modification car la réponse du propriétaire ne peut pas être vérifiée. Root utilise un auxiliaire sous UID système uniquement pour l'utilisateur 0 sur les appareils compatibles. Les règles persistantes concurrentes ne sont pas remplacées. `persistentConfigured` atteste une ancienne configuration réussie, sans prouver la règle système actuelle; l'observation passive indique seulement `preferred` ou `none`.
- `dhizuku`: nécessite Android 8.0 (API 26)+, un propriétaire d'appareil/profil Dhizuku actif et l'autorisation donnée au plugin. Les opérations sont limitées à l'utilisateur du propriétaire courant; l'attribution de l'installation utilise le vrai paquet propriétaire. Les options shell/root de rétrogradation, paquet de test, contournement du targetSdk, autres utilisateurs, attribution arbitraire et conservation des données à la désinstallation sont refusées. Le plugin ne configure pas de propriétaire.

******

### Questions fréquentes

******

- **Pourquoi une confirmation reste-t-elle nécessaire?** `none` exige toujours la confirmation Android. Les méthodes privilégiées restent soumises à la politique Android. `notification` ouvre la confirmation système uniquement par son action de notification et ne la contourne pas.
- **Peut-on installer un `.aab` ?** Non. Un Android App Bundle est un format de publication ; convertissez-le d'abord avec bundletool en un ensemble `.apks`. Le plugin reconnaît les fichiers `.aab` et affiche les informations de paquet et de modules.
- **Pourquoi une rétrogradation peut-elle échouer avec `allowDowngrade: true`?** Ce paramètre demande une rétrogradation; Android décide selon le firmware, l'identité autorisée et le caractère debuggable de l'application. Les firmwares user testés sur Sony G8441 / API 28 et Xiaomi 23046RP50C / API 35 ont refusé les rétrogradations d'applications non debuggable, tandis que Sony XQ-DQ72 / API 33 avec Root en a accepté une. Ces résultats concernent ces appareils uniquement. Consultez l'erreur et `systemMessage`; Root ne garantit pas une rétrogradation sur toutes les ROM.
- **Quel nom de paquet d'installateur utiliser sur HyperOS?** Avec Shizuku lancé par ADB ou le débogage sans fil, ne pas spécifier de nom utilise `com.android.shell`. Sur le Xiaomi 23046RP50C / HyperOS / API 35 testé, les nouvelles installations et mises à jour silencieuses ont enregistré cette valeur. Les valeurs explicites `com.android.shell` et le nom du paquet du plugin ont aussi été acceptées et enregistrées comme demandé. Les autres noms ou versions de ROM restent soumis à la réponse du système.
- **ColorOS ou une autre ROM indique que le plugin doit être activé. Que faire?** Après une installation ou un arrêt forcé, Android peut maintenir l'application à l'arrêt jusqu'à une interaction de l'utilisateur. Utilisez l'action Activer dans le centre de plugins AutoJs6 si elle est proposée, ou ouvrez 3-Setup Installer depuis son icône, puis réessayez. Cela suit les [règles d'arrêt d'Android](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). Le comportement propre à ColorOS n'a pas encore été vérifié sur un appareil.
- **Pourquoi le changement d'installateur peut-il échouer, ou l'indication persistante différer du gestionnaire actuel?** La page de l'installateur par défaut distingue préférence ordinaire et règle persistante. La préférence utilise Shizuku ou Root et reste soumise à la ROM. Dhizuku gère les règles persistantes sur API 26-33; API 34+ est refusé avant toute modification car la réponse du propriétaire ne peut pas être vérifiée. Root utilise un auxiliaire sous UID système uniquement pour l'utilisateur 0 sur les appareils compatibles. Les règles persistantes concurrentes ne sont pas remplacées. `persistentConfigured` atteste une ancienne configuration réussie, sans prouver la règle système actuelle; l'observation passive indique seulement `preferred` ou `none`.
- **Pourquoi la source n'a-t-elle pas été supprimée?** La suppression n'est tentée qu'après une installation réussie. En cas d'échec, d'annulation ou de dépassement du délai d'installation, la source est toujours conservée. Un échec de suppression ne change pas une installation réussie, et le fournisseur externe peut refuser la suppression. Pour les scripts, l'hôte applique `deleteSource` aux chemins et sources `file://`, en conservant les sources `content://`. Consultez `sourceDeleted` et `notes`. Dans un lot, les éléments dont le succès est confirmé suivent toujours `deleteSource`, même si un autre échoue ou si le reste de la file est annulé.
- **Puis-je réessayer ou reprendre?** Un URI externe en échec peut être réessayé tant que la source et son accès sont disponibles. Si la source ou l'accès a été libéré, rouvrez le paquet. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative. Vérifiez l'application installée avant de recommencer.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul AutoJs6 peut donc les atteindre ; l'entrée externe "Ouvrir avec" n'accepte que des fichiers de paquet et n'exécute jamais de script.
- REQUEST_INSTALL_PACKAGES et REQUEST_DELETE_PACKAGES permettent la confirmation Android. QUERY_ALL_PACKAGES sert à gérer les applications installées, comparer versions et signatures, et détecter l'installateur par défaut.
- FOREGROUND_SERVICE et FOREGROUND_SERVICE_DATA_SYNC soutiennent l'installation et l'accès temporaire aux sources; POST_NOTIFICATIONS autorise les notifications. Le mode `notification` exige des notifications et un canal actifs. Les autres modes tolèrent une permission de notification absente.
- Shizuku, Root et Dhizuku servent aux opérations demandées. Le plugin ne configure aucun propriétaire d'appareil/profil. Les règles persistantes ne changent que sur demande de définition ou de suppression; aucun paquet n'est envoyé en ligne.
- L'installation, l'inspection, l'historique et la gestion des applications fonctionnent hors ligne. INTERNET est utilisé uniquement lors d'une recherche manuelle de versions auprès de l'API GitHub Releases fixe du plugin, avec un intervalle de 12 heures. Aucune recherche en arrière-plan ni aucun envoi de paquets n'est effectué.
- Les sources sont ouvertes en lecture seule. L'historique conserve des métadonnées limitées et des résultats, sans contenu de paquet ni URI source; les chemins des erreurs sont masqués. Le stockage privé est exclu des sauvegardes. Supprimer une entrée ne désinstalle pas l'application et ne supprime pas sa source.

Après publication, n'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

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

`ThreeSetupInstallerPluginService` répond à `org.autojs.plugin.INSTALLER` (category `installer`) et implémente le contrat installer-api de l'hôte `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` répond à `org.autojs.plugin.INFO` avec PluginInfo. `WakeActivity` permet à l'hôte d'activer le plugin.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.1.0

_2026/10/02_

- `Fonctionnalité` `dhizuku`: nécessite Android 8.0 (API 26)+, un propriétaire d'appareil/profil Dhizuku actif et l'autorisation donnée au plugin. Les opérations sont limitées à l'utilisateur du propriétaire courant; l'attribution de l'installation utilise le vrai paquet propriétaire. Les options shell/root de rétrogradation, paquet de test, contournement du targetSdk, autres utilisateurs, attribution arbitraire et conservation des données à la désinstallation sont refusées. Le plugin ne configure pas de propriétaire.
- `Fonctionnalité` La page de l'installateur par défaut distingue préférence ordinaire et règle persistante. La préférence utilise Shizuku ou Root et reste soumise à la ROM. Dhizuku gère les règles persistantes sur API 26-33; API 34+ est refusé avant toute modification car la réponse du propriétaire ne peut pas être vérifiée. Root utilise un auxiliaire sous UID système uniquement pour l'utilisateur 0 sur les appareils compatibles. Les règles persistantes concurrentes ne sont pas remplacées. `persistentConfigured` atteste une ancienne configuration réussie, sans prouver la règle système actuelle; l'observation passive indique seulement `preferred` ou `none`.
- `Fonctionnalité` `interaction: 'notification'` concerne uniquement l'installation: les notifications présentent confirmation initiale, annulation, progression et résultats sans dialogues d'installation du plugin. La confirmation Android exige toujours un appui sur sa notification. La permission, les notifications de l'application et le canal d'installation doivent être activés; sinon la requête échoue avec `NOTIFICATION_UNAVAILABLE`. Les autres modes tolèrent l'absence de permission de notification. La désinstallation n'accepte pas `notification`.
- `Amélioration` `none` utilise la confirmation Android. Les nouveaux paramètres essaient `shizuku -> root -> dhizuku -> none` pour `auto`, selon leur disponibilité; chaque méthode peut être déplacée ou désactivée. Les anciens paramètres à trois méthodes conservent leur ordre relatif et leurs activations, avec Dhizuku inséré avant `none` mais désactivé. Un choix explicite ne bascule jamais vers une autre méthode.
- `Amélioration` Les paramètres enregistrent l'ordre et l'activation des méthodes, les options d'installation et les préférences de notification. Les installations locales/externes utilisent `dialog` par défaut; `auto`, `silent` ou `notification` peuvent être choisis explicitement. L'interface hôte utilise `dialog`; les scripts gardent leurs options explicites et `auto` par défaut. Les changements sont enregistrés après confirmation.
- `Dépendance` Ajout de Dhizuku API 2.6.0 (MIT) pour l'autorisation par propriétaire d'appareil/profil
- `Dépendance` Mise à niveau de `installer-api.aar` vers le contrat V2 (MPL 2.0), avec négociation V1 conservée et méthode persistante ajoutée à la fin; origine et SHA-256 figurent dans les avis de tiers; AutoJs6 >= 6.8.0 (5307).

#### v1.0.0

_2026/10/02_

- `Note` 1.0.0 décrit les fonctions d'installation, de gestion des applications et de scripts présentées ci-dessous. La publication officielle sur GitHub Releases et le référencement dans le centre de plugins restent à effectuer. L'intégration nécessite AutoJs6 >= 6.8.0 (5299), et l'API de scripts `installer` nécessite la compilation 5300 ou ultérieure. La couverture des appareils et les validations restantes figurent dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- `Fonctionnalité` 3-Setup Installer installe, met à jour, inspecte et désinstalle des applications Android depuis son accueil autonome, les entrées et scripts AutoJs6, ou l'ouverture et le partage de paquets par d'autres applications. Il prend en charge la confirmation Android et les opérations privilégiées via Shizuku ou Root
- `Fonctionnalité` Formats de paquet : `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` et archives ZIP contenant des APK ; les paquets fractionnés sont sélectionnés pour l'appareil ; les fichiers `.aab` sont reconnus et décrits mais pas installés
- `Fonctionnalité` La suppression de la source après une installation réussie peut être demandée sans garantie. Les rétrogradations, paquets de test, contournements du targetSdk minimal (Android 14+), attributions d'installateur et autres utilisateurs nécessitent Shizuku ou Root et restent soumis aux restrictions Android
- `Fonctionnalité` L'API de scripts `installer` (alias `$installer`) propose les formes synchrone, `...Async` et session pour l'installation simple, par lots ou fractionnée, la désinstallation, l'inspection, les requêtes d'autorisation et d'utilisateurs, et l'installateur par défaut. Les erreurs sont des objets `InstallerError` avec un `code` stable (nécessite AutoJs6 >= 6.8.0 (5300)). Les scripts utilisent `interaction: 'auto'` par défaut et installent silencieusement si les privilèges sont disponibles. Les actions d'installation de l'interface hôte utilisent `dialog`. Si Android exige une confirmation, `auto` l'autorise et la consigne dans `notes`. Choisissez `interaction: 'dialog'` pour confirmer avant l'installation. Le choix explicite `silent` échoue avec `AUTHORIZER_REQUIRED` si les privilèges manquent ou si une confirmation système est nécessaire
- `Fonctionnalité` L'accueil autonome affiche la disponibilité et les autorisations Shizuku/Root, l'installateur par défaut, les tâches actives et les installations récentes. Sélectionnez plusieurs paquets dans le sélecteur système pour les installer successivement, continuer après un échec individuel ou annuler les éléments restants
- `Fonctionnalité` La confirmation affiche les informations de l'application, les versions ancienne et nouvelle, les signatures et les composants APK sélectionnables. La progression permet l'annulation; les résultats proposent les actions de réussite ou les erreurs à copier. Les installations par lots affichent chaque état séparément
- `Fonctionnalité` Notifications de progression au premier plan, d'annulation et de résultats. Refuser la permission de notification n'empêche pas l'installation
- `Fonctionnalité` Ouvrez ou partagez un ou plusieurs paquets, y compris les fichiers APKS partagés par MT Manager. Plusieurs paquets rejoignent une file séquentielle. Les éléments externes en échec peuvent être réessayés tant que leur URI et leur accès restent disponibles
- `Fonctionnalité` La liste des applications installées permet une recherche par nom ou nom de paquet, un tri par nom, date d'installation ou de mise à jour, et l'affichage des applications système. Ouvrez une application ou sa fiche système, ou vérifiez puis confirmez sa désinstallation. Shizuku ou Root permettent ensuite de désinstaller sans nouvelle confirmation système, avec conservation facultative des données; les autres cas utilisent la confirmation Android
- `Fonctionnalité` La carte d'accueil et les paramètres ouvrent la même page d'installateur par défaut, avec définition et suppression privilégiées ou indications vers les paramètres système sans privilèges. Les politiques OEM peuvent bloquer le changement ou imposer d'effacer l'ancien choix. Les scripts conservent `installer.isDefault`, `installer.setDefault` et `setDefaultAsync`; les résultats reflètent la réponse de l'appareil
- `Fonctionnalité` Les paramètres enregistrent l'ordre et l'activation des autorisations, les options d'installation et les notifications de progression. Les installations locales et externes utilisent `dialog` par défaut; les choix `auto` ou `silent` enregistrés explicitement s'appliquent. Les requêtes hôte/script conservent leurs options explicites, et l'API de script garde `auto` par défaut. Les choix ne sont enregistrés qu'après confirmation
- `Fonctionnalité` L'apparence comprend la langue, le mode nuit, la couleur du thème et l'icône du lanceur. Les trois premiers suivent AutoJs6 par défaut et acceptent des choix locaux; sans hôte, la langue et le mode du système ainsi que la couleur par défaut s'appliquent. Les icônes proposent les modes clair, sombre, automatique et transparent; le mode automatique suit le système, selon le cache et les masques du lanceur
- `Fonctionnalité` L'historique privé conserve au plus 200 éléments: paquet, nom, anciennes/nouvelles versions, résultat, date, origine (hôte/script/externe/accueil), autorisation et détails d'échec. Supprimez une entrée ou videz l'historique sans désinstaller les applications ni supprimer les sources. Après l'arrêt du processus, les éléments inachevés deviennent annulés sans reprise automatique
- `Fonctionnalité` Les paramètres donnent accès à la page À propos et à l'historique des versions intégré en dix langues. La recherche manuelle de mises à jour utilise l'API GitHub Releases du plugin avec un intervalle de 12 heures, un cache et la gestion des versions ignorées. Les pages de publication s'ouvrent dans le navigateur; aucune mise à jour n'est téléchargée ni installée automatiquement
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Amélioration` Les sources à accès aléatoire évitent une copie complète en cache, tandis que les flux sont stockés temporairement si nécessaire. Les paquets fractionnés en ZIP sont pris en charge, les AAB restent limités à l'inspection et les sources modifiées sont refusées
- `Amélioration` La suppression n'est tentée qu'après une installation réussie. En cas d'échec, d'annulation ou de dépassement du délai d'installation, la source est toujours conservée. Un échec de suppression ne change pas une installation réussie, et le fournisseur externe peut refuser la suppression. Pour les scripts, l'hôte applique `deleteSource` aux chemins et sources `file://`, en conservant les sources `content://`. Consultez `sourceDeleted` et `notes`. Dans un lot, les éléments dont le succès est confirmé suivent toujours `deleteSource`, même si un autre échoue ou si le reste de la file est annulé
- `Amélioration` Installation du même paquet en série entre utilisateurs et modes de privilèges, avec annulation et délai pendant l'attente, et nettoyage sûr des dossiers temporaires inactifs depuis 24 heures au démarrage autonome
- `Amélioration` Réessayer une fois une connexion privilégiée interrompue pendant son établissement; les installations et désinstallations déjà commencées ne sont jamais répétées automatiquement
- `Dépendance` Ajout de Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) pour le mode d'autorisation Shizuku
- `Dépendance` Ajout de libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) pour le mode d'autorisation Root
- `Dépendance` Ajout de AndroidHiddenApiBypass 6.1 pour les API cachées de l'installateur de paquets utilisées par le service privilégié
- `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5298, MPL 2.0) comme contrat de plugin partagé, avec hachage verrouillé dans `locks/host-api-aars.lock`
- `Dépendance` Ajout de `installer-api.aar` (AutoJs6, MPL 2.0) pour le contrat d'installation; origine et SHA-256 figurent dans les avis de tiers
- `Dépendance` Ajout de `package-archive-parser.aar` (AutoJs6, MPL 2.0) pour l'inspection des APK et conteneurs et la sélection des parties; origine et SHA-256 figurent dans les avis de tiers

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation et vérification

******

Les développeurs peuvent compiler et vérifier le plugin avec les commandes ci-dessous. Avant la publication officielle, testez une compilation fournie par le mainteneur ou une compilation locale. Les APK publiés seront distribués via Releases et le centre de plugins après référencement.

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
