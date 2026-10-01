3-Setup Installer installe, met à jour, inspecte et désinstalle les applications Android via les fonctions d'installation d'AutoJs6 et les demandes externes d'ouverture ou de partage de paquets. Il prend en charge la confirmation Android habituelle et l'installation privilégiée avec Shizuku ou Root. L'API de scripts et les pages autonomes d'accueil et de paramètres restent prévues.

1.0.0: Aperçu de développement P3. Les dialogues de confirmation, progression, résultats et lots, l'ouverture et le partage externes, la suppression facultative de la source, la confirmation système et les notifications de premier plan sont implémentés. Après un redémarrage du processus, la vue restaurée affiche les résultats confirmés enregistrés et marque les éléments non terminés comme interrompus. Elle est en lecture seule et ne lance jamais automatiquement une installation ou une nouvelle tentative. L'API de scripts, l'accueil et les paramètres autonomes, l'historique et la configuration de l'installateur par défaut restent prévus. Consultez [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour la progression et les appareils couverts. AutoJs6 >= 6.8.0 (5299).

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur un appareil doté d'AutoJs6 build 5299 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `3-Setup Installer` est reconnu et activez-le.
3. Utilisez une action d'installation d'AutoJs6 ou choisissez 3-Setup Installer pour ouvrir ou partager des paquets. Si une confirmation apparaît, vérifiez l'application et les options avant d'installer. Préparez l'autorisation Shizuku ou Root si vous choisissez une méthode privilégiée.

### Modes d'autorisation

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku`: nécessite Shizuku en cours d'exécution (démarré par débogage sans fil, ADB ou Root) et la permission accordée au plugin. Les droits shell permettent l'installation et la désinstallation silencieuses, ainsi que les opérations pour d'autres utilisateurs.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.
- **Remarque:** Lorsque les privilèges sont disponibles, les demandes de l'hôte avec `interaction: 'auto'` installent silencieusement sans ouvrir d'abord une confirmation. Si Android exige une confirmation, `auto` l'autorise et la consigne dans `notes`. Utilisez `interaction: 'dialog'` pour confirmer avant l'installation, ou `interaction: 'silent'` pour échouer si une confirmation système est nécessaire. L'API de scripts prévue suivra le même comportement par défaut.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour le guide d'installation et l'avancement actuel.
