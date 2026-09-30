3-Setup Installer reprend l'installateur de paquets d'AutoJs6 : les boutons d'installation du gestionnaire de fichiers, du centre de plugins et du générateur de scripts empaquetés, l'entrée externe "Ouvrir avec" pour les fichiers `.apk`, `.apks`, `.xapk`, `.apkm` et `.apkz`, ainsi que l'objet global `installer` côté script pour installer, mettre à jour, inspecter et désinstaller des applications. En plus de la confirmation système habituelle, il peut installer et désinstaller silencieusement via Shizuku ou Root.

La version 1.0.0 est l'aperçu de développement P0 : le squelette du dépôt, l'identité du plugin reconnue par le centre de plugins AutoJs6 et la validation (spike) de l'installation privilégiée. Le contrat Binder, le moteur d'installation, les boîtes de dialogue, l'API de script et la page des paramètres suivent les phases de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). Nécessite AutoJs6 6.8.0 (build 5299) ou plus récent. P0 a validé l'installation silencieuse, les mises à jour, la désinstallation et le choix ordinaire de l'installateur par défaut avec Shizuku et Root. Les points d'installation depuis l'hôte et les scripts ne sont pas encore disponibles; les valeurs par défaut persistantes restent hors de cette version.

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur un appareil doté d'AutoJs6 build 5299 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `3-Setup Installer` est reconnu et activez-le.
3. Touchez un fichier de paquet dans le gestionnaire de fichiers AutoJs6, ouvrez un paquet depuis n'importe quel gestionnaire de fichiers avec 3-Setup Installer, ou appelez `installer.install(...)` depuis un script. Pour une installation silencieuse, démarrez Shizuku ou accordez Root lorsque le plugin le demande, ou choisissez le mode d'autorisation dans les paramètres du plugin.

### Modes d'autorisation

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku` : nécessite l'application Shizuku en cours d'exécution (démarrée via le débogage sans fil, ADB ou Root) et la permission accordée au plugin ; s'exécute avec les droits shell, qui permettent l'installation silencieuse, la désinstallation silencieuse, les autres utilisateurs et le verrouillage de l'installateur par défaut.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour le guide d'installation et l'avancement actuel.
