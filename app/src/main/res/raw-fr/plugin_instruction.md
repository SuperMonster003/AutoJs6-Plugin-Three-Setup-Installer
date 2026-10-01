3-Setup Installer reprend l'installateur de paquets d'AutoJs6 : les boutons d'installation du gestionnaire de fichiers, du centre de plugins et du générateur de scripts empaquetés, l'entrée externe "Ouvrir avec" pour les fichiers `.apk`, `.apks`, `.xapk`, `.apkm` et `.apkz`, ainsi que l'objet global `installer` côté script pour installer, mettre à jour, inspecter et désinstaller des applications. En plus de la confirmation système habituelle, il peut installer et désinstaller silencieusement via Shizuku ou Root.

1.0.0: Aperçu de développement P2: installation, inspection, requêtes utilisateur et désinstallation sont reliées au service hôte, avec confirmation explicite et nettoyage automatique des sessions. La validation complète des entrées hôtes, l'interface complète, l'ouverture externe, l'activation comme installateur par défaut, l'API de script et les paramètres restent en cours. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur un appareil doté d'AutoJs6 build 5299 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `3-Setup Installer` est reconnu et activez-le.
3. Touchez un fichier de paquet dans le gestionnaire de fichiers AutoJs6, ouvrez un paquet depuis n'importe quel gestionnaire de fichiers avec 3-Setup Installer, ou appelez `installer.install(...)` depuis un script. Pour une installation silencieuse, démarrez Shizuku ou accordez Root lorsque le plugin le demande, ou choisissez le mode d'autorisation dans les paramètres du plugin.

### Modes d'autorisation

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku` : nécessite l'application Shizuku en cours d'exécution (démarrée via le débogage sans fil, ADB ou Root) et la permission accordée au plugin ; s'exécute avec les droits shell, qui permettent l'installation silencieuse, la désinstallation silencieuse, les autres utilisateurs et le verrouillage de l'installateur par défaut.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.
- **Remarque:** Quand les privilèges sont disponibles, l'API de script installe silencieusement par défaut et n'affiche aucun dialogue de confirmation de sa propre initiative. Si Android exige une confirmation, `interaction: 'auto'` autorise le dialogue système et l'indique dans `notes`. Utilisez explicitement `interaction: 'dialog'` pour confirmer avant l'installation, ou `interaction: 'silent'` pour échouer au lieu d'afficher une confirmation système.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour le guide d'installation et l'avancement actuel.
