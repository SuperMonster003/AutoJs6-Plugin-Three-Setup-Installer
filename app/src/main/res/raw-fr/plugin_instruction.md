3-Setup Installer installe, met à jour, inspecte et désinstalle des applications Android. Une installation peut partir de son écran d'accueil, des actions d'installation et des scripts d'AutoJs6, ou des paquets ouverts ou partagés par d'autres applications; Android confirme chacune par défaut, et avec Shizuku, Root ou Dhizuku autorisé, l'installation silencieuse est aussi possible. Il fonctionne aussi seul, sans AutoJs6.

### Prise en main

1. Ouvrez 3-Setup Installer depuis le lanceur. Pour l'installation silencieuse, touchez "Autoriser" pour Shizuku, Root ou Dhizuku dans "Autorisation" sur l'écran d'accueil.
2. Touchez "Choisir des paquets à installer" et sélectionnez un ou plusieurs paquets APK, APKS, XAPK ou similaires.
3. Vérifiez la version, la signature et les permissions dans la boîte de dialogue d'installation, puis touchez "Installer".
4. Une fois ce plugin activé dans le "Centre des plugins" d'AutoJs6, les actions d'installation d'AutoJs6 affichent la même boîte de dialogue; 3-Setup Installer peut aussi être choisi lorsqu'une autre application ouvre ou partage un paquet.

### Fonctionnalités principales

- **Boîte de dialogue d'installation**: affiche le changement de version, la taille, la comparaison du SDK et de la signature, et distingue nouvelle installation, mise à jour, réinstallation ou version antérieure; les composants APK, les permissions et "Plus d'options" peuvent être développés et ajustés.
- **Autorisation**: sans autorisation, Android confirme chaque installation; avec Shizuku, Root ou Dhizuku, l'installation peut être silencieuse, accepter une version antérieure, accorder des permissions, etc. "Ordre et activation des autorisations" dans les paramètres fixe l'ordre de la sélection automatique.
- **Lots et historique**: plusieurs applications peuvent être installées d'un coup, et l'écran d'accueil affiche les tâches en cours et les "Installations récentes"; l'historique ne garde que les informations des applications et les résultats, jamais les paquets.
- **Applications installées**: accessible depuis le menu "Plus" de l'écran d'accueil, pour rechercher, trier, consulter les informations d'une application ou la désinstaller.
- **Installateur par défaut**: définit ce plugin comme application par défaut pour ouvrir les fichiers APK; la possibilité dépend de l'autorisation et des restrictions du système.
- **Profils d'installation et listes noires**: prédéfinissez dans les paramètres des options d'installation par source et par préfixe de nom de paquet, ou bloquez l'installation d'applications précises avec "Liste noire des paquets" et "Liste noire des UID partagés".
- **Contrôle de la signature**: un paquet dont la signature diffère de celle de l'application installée est bloqué par défaut et ne peut être autorisé qu'une fois, pour cet élément, dans la boîte de dialogue d'installation.
- **Paramètres**: réglez "Interaction par défaut", l'apparence, l'icône du lanceur et les notifications, ou consultez "Historique des versions" et lancez "Rechercher les mises à jour" manuellement.

### Utilisation dans les scripts

Les scripts AutoJs6 installent, inspectent et désinstallent des applications avec le module `installer`. L'exemple ci-dessous affiche la boîte de dialogue d'installation et installe après confirmation de l'utilisateur; les méthodes synchrones doivent être appelées sur un thread de script, pas sur le thread UI:

```js
console.log(installer.status);
try {
    let result = installer.install('./app.apk', { interaction: 'dialog' });
    console.log(result.packageName, result.versionName);
} catch (e) {
    console.error(e.code, e.message);
}
```

- Sans `interaction`, le mode est `auto`: l'installation est silencieuse si une autorisation privilégiée est disponible, sinon Android demande une confirmation.
- Un tableau lance une installation par lots et renvoie les résultats dans l'ordre des sources; vérifiez `ok` et `error` pour chaque élément. `installAsync` renvoie une Promise et peut être utilisé sur le thread UI.
- `installer.inspect(source)` lit seulement le nom du paquet, la version et d'autres informations, sans installer.
- Toutes les options et tous les événements sont décrits dans [Documentation AutoJs6 > installer](https://docs.autojs6.com/#/installer).

### Prérequis

- Android 7.0 ou version ultérieure. L'utilisation avec AutoJs6 requiert AutoJs6 6.8.0 ou version ultérieure; l'utilisation seule ne nécessite pas AutoJs6.
- Shizuku, Root et Dhizuku sont facultatifs et ne servent qu'à l'installation silencieuse ou aux options privilégiées; Dhizuku requiert Android 8.0 ou version ultérieure.
- Les permissions d'installation et de désinstallation demandent la confirmation d'Android, celle de la liste des applications sert aux applications installées, aux versions et à la comparaison des signatures, et celle des notifications à l'installation par notification et aux notifications de progression.
- Le réseau n'est utilisé que pour rechercher manuellement les mises à jour du plugin; les paquets ne sont jamais envoyés.

### Questions fréquentes

**Pourquoi une confirmation reste nécessaire après autorisation**

La politique d'Android ou du fabricant peut toujours exiger une confirmation. Dans ce cas, "Automatique" passe à la confirmation d'Android, tandis qu'un mode silencieux échoue en indiquant la raison.

**Pourquoi le service d'accessibilité ne peut pas être activé après l'installation**

Depuis Android 13, les applications installées depuis un fichier local ne peuvent pas utiliser l'accessibilité et les autres paramètres restreints; les installations via Shizuku ou Root ne sont pas marquées comme fichier local par défaut. Pour une application de confiance, autorisez les paramètres restreints depuis le menu en haut à droite de sa page d'informations dans les paramètres système, puis activez son service d'accessibilité; l'emplacement varie selon le système.

**Pourquoi les fichiers .aab ne peuvent pas être installés**

Un Android App Bundle est un format de publication et ne s'installe pas directement. Convertissez-le d'abord en paquet .apks avec bundletool; le plugin peut afficher le nom du paquet et les modules d'un fichier .aab.

**Pourquoi une version antérieure échoue même autorisée**

"Autoriser une version antérieure" ne fait que demander au système; l'acceptation dépend de la version du système, de l'autorisation et du caractère débogable de l'application. En cas d'échec, le résultat contient le message renvoyé par le système.

Les détails sur les autorisations, les options d'installation et les permissions figurent sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer).
