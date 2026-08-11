# Cahier des charges --- Application de gestion d'une pharmacie

## 1. Présentation du projet

### 1.1 Intitulé

**Projet 1 : Gestion d'une pharmacie**

### 1.2 Objectif général

L'objectif est de développer une application desktop de gestion d'une
pharmacie permettant de gérer les médicaments, les entrées de stock, les
achats des clients et les indicateurs de gestion.

L'interface graphique devra être réalisée **exclusivement avec Java
Swing**. Le projet sera développé dans **Visual Studio Code (VS Code)**.

L'application devra avoir une interface visuelle nette, claire,
structurée et facile à utiliser.

### 1.3 Fonctionnalités principales

L'application sera organisée autour de :

-   une fenêtre de **connexion (Login)** ;
-   une **fenêtre principale** ;
-   un **Dashboard** ;
-   un menu **STOCK** ;
-   un menu **ACHAT** ;
-   un menu **BILAN** ;
-   les opérations CRUD sur les données ;
-   la recherche de médicaments ;
-   la détection des stocks insuffisants ;
-   la génération d'une facture PDF après un achat ;
-   les statistiques de vente et de recette.

------------------------------------------------------------------------

# 2. Contraintes techniques

## 2.1 Technologie obligatoire

-   **Langage : Java**
-   **Interface graphique : Java Swing uniquement**
-   **IDE : Visual Studio Code**
-   **Architecture recommandée : séparation interface / logique / accès
    aux données**
-   **Base de données : à connecter via JDBC si une base de données est
    utilisée**
-   **Facture : génération au format PDF**

> JavaFX, React, Angular, HTML/CSS comme interface principale ou autres
> frameworks graphiques ne sont pas utilisés. L'interface utilisateur
> doit être construite avec les composants Swing (`JFrame`, `JPanel`,
> `JTable`, `JTextField`, `JButton`, `JComboBox`, etc.).

## 2.2 Principes visuels

L'application doit privilégier :

-   une interface propre et professionnelle ;
-   une navigation simple ;
-   des menus clairement identifiables ;
-   des tableaux lisibles ;
-   des boutons avec des intitulés explicites ;
-   une bonne séparation entre les zones de saisie et les zones
    d'affichage ;
-   des messages d'erreur compréhensibles ;
-   des confirmations avant les suppressions ;
-   une présentation homogène de toutes les fenêtres.

------------------------------------------------------------------------

# 3. Données manipulées par l'application

Le document de projet définit trois tables principales.

## 3.1 Table MEDICAMENT

Structure :

  Champ             Type     Description
  ----------------- -------- ------------------------------
  `numMedoc`        String   Identifiant du médicament
  `Design`          String   Désignation du médicament
  `prix_unitaire`   int      Prix unitaire
  `stock`           int      Quantité disponible en stock

### Règle importante

Le **stock initial d'un médicament doit toujours être égal à 0**.

Le stock augmente ensuite à chaque entrée de stock.

Source du projet : le stock initial est fixé à 0 et s'additionne à
chaque entrée. fileciteturn0file0L3-L6

------------------------------------------------------------------------

## 3.2 Table ENTREE

Structure :

  Champ           Type     Description
  --------------- -------- -------------------------
  `numEntree`     String   Identifiant de l'entrée
  `numMedoc`      String   Médicament concerné
  `stockEntree`   int      Quantité entrée
  `dateEntree`    date     Date de l'entrée

Une entrée permet d'augmenter le stock d'un médicament.

------------------------------------------------------------------------

## 3.3 Table ACHAT

Structure :

  Champ         Type     Description
  ------------- -------- ------------------------
  `numAchat`    String   Identifiant de l'achat
  `numMedoc`    String   Médicament acheté
  `nomClient`   String   Nom du client
  `nbr`         int      Quantité achetée
  `dateAchat`   date     Date de l'achat

Le document précise qu'un client peut acheter **un ou plusieurs
médicaments** et que chaque achat doit diminuer le stock correspondant.
fileciteturn0file0L7-L11

> Pour l'interface, il est donc recommandé de permettre d'ajouter
> plusieurs lignes de médicaments dans un même achat avant de valider la
> vente.

------------------------------------------------------------------------

# 4. Architecture générale de l'application

L'application sera divisée en plusieurs zones fonctionnelles.

``` text
APPLICATION PHARMACIE
│
├── LOGIN
│
└── FENÊTRE PRINCIPALE
    │
    ├── DASHBOARD
    │
    ├── STOCK
    │   ├── Médicaments
    │   └── Entrées de stock
    │
    ├── ACHAT
    │   ├── Nouvelle vente
    │   ├── Historique des achats
    │   └── Facture
    │
    └── BILAN
        ├── Recette totale
        ├── Médicaments les plus vendus
        ├── Ruptures / stocks faibles
        └── Recettes des 5 derniers mois
```

------------------------------------------------------------------------

# 5. Écran LOGIN

## 5.1 Objectif

Le Login est la première fenêtre affichée au lancement de l'application.

Il permet de contrôler l'accès à la fenêtre principale.

## 5.2 Organisation visuelle

La fenêtre doit être centrée et contenir :

-   titre de l'application ;
-   éventuellement logo ou icône de pharmacie ;
-   champ **Nom d'utilisateur** ;
-   champ **Mot de passe** ;
-   bouton **Se connecter** ;
-   bouton **Quitter**.

Exemple d'organisation :

``` text
┌───────────────────────────────────────┐
│          GESTION PHARMACIE            │
│                                       │
│        Nom d'utilisateur              │
│        [____________________]         │
│                                       │
│        Mot de passe                   │
│        [____________________]         │
│                                       │
│       [ SE CONNECTER ] [ QUITTER ]    │
└───────────────────────────────────────┘
```

## 5.3 Comportement

-   Si les informations sont correctes : ouverture de la fenêtre
    principale.
-   Si les informations sont incorrectes : affichage d'un message
    d'erreur.
-   Le mot de passe doit être saisi avec un `JPasswordField`.
-   Le bouton Quitter ferme l'application.

------------------------------------------------------------------------

# 6. FENÊTRE PRINCIPALE

## 6.1 Structure générale

La fenêtre principale doit être la fenêtre centrale de l'application
après connexion.

Organisation recommandée :

``` text
┌─────────────────────────────────────────────────────────────┐
│  PHARMACIE                     Utilisateur     [Déconnexion] │
├──────────────┬──────────────────────────────────────────────┤
│              │                                              │
│ DASHBOARD    │                                              │
│              │              CONTENU                         │
│ STOCK        │                                              │
│              │              DU MENU                         │
│ ACHAT        │                                              │
│              │                                              │
│ BILAN        │                                              │
│              │                                              │
├──────────────┴──────────────────────────────────────────────┤
│              Gestion de pharmacie                           │
└─────────────────────────────────────────────────────────────┘
```

## 6.2 Composants Swing recommandés

-   `JFrame` : fenêtre principale ;
-   `JPanel` : zones de l'interface ;
-   `JButton` : navigation ;
-   `JLabel` : titres et informations ;
-   `JTable` : affichage des médicaments, entrées et achats ;
-   `JTextField` : saisie ;
-   `JComboBox` : sélection d'un médicament ;
-   `JSpinner` ou champ numérique : quantités ;
-   `JDateChooser` uniquement si une bibliothèque externe est autorisée
    ; sinon utiliser les composants Swing/Java standards pour la saisie
    de date ;
-   `JOptionPane` : confirmations et messages ;
-   `CardLayout` : changement de contenu dans la zone centrale.

------------------------------------------------------------------------

# 7. DASHBOARD

Le Dashboard est la page d'accueil après connexion.

Il doit donner une vision rapide de l'état de la pharmacie.

## 7.1 Indicateurs à afficher

Le Dashboard devra notamment présenter :

### Carte 1 --- Nombre de médicaments

Afficher le nombre de médicaments enregistrés.

### Carte 2 --- Stock faible

Afficher le nombre de médicaments dont le stock est inférieur à 5.

Le cahier des charges demande la liste des médicaments en rupture/faible
stock lorsque la quantité est **inférieure à 5**.
fileciteturn0file0L15-L17

### Carte 3 --- Recette totale

Afficher la recette totale accumulée par la pharmacie.

### Carte 4 --- Achats

Afficher le nombre d'achats enregistrés.

## 7.2 Zone « Médicaments les plus vendus »

Afficher un tableau contenant les **5 médicaments les plus vendus**.

Le projet demande explicitement l'affichage des cinq médicaments les
plus vendus. fileciteturn0file0L17-L19

## 7.3 Zone « Alertes stock »

Afficher les médicaments dont le stock est inférieur à 5.

Exemple :

  Médicament      Stock État
  ------------- ------- --------------
  Paracétamol         3 Stock faible
  Vitamine C          2 Stock faible

------------------------------------------------------------------------

# 8. MENU STOCK

Le menu STOCK regroupe tout ce qui concerne les médicaments et les
entrées de stock.

Il doit être divisé en deux parties :

``` text
STOCK
├── Médicaments
└── Entrées de stock
```

------------------------------------------------------------------------

## 8.1 STOCK → MÉDICAMENTS

### Objectif

Permettre la gestion complète de la table MEDICAMENT.

Le projet demande les opérations :

-   création ;
-   listage ;
-   suppression ;
-   modification.

Ces opérations CRUD sont demandées pour les trois tables.
fileciteturn0file0L12-L14

### Interface

La fenêtre peut être organisée ainsi :

``` text
┌─────────────────────────────────────────────────────────────┐
│ GESTION DES MÉDICAMENTS                                    │
├─────────────────────────────────────────────────────────────┤
│ Numéro :       [____________]                              │
│ Désignation :  [____________]                              │
│ Prix unitaire: [____________]                              │
│ Stock :        [ 0 ]                                       │
│                                                             │
│ [Ajouter] [Modifier] [Supprimer] [Vider]                  │
├─────────────────────────────────────────────────────────────┤
│ Recherche : [____________________] [Rechercher]             │
├─────────────────────────────────────────────────────────────┤
│ TABLE DES MÉDICAMENTS                                      │
│                                                             │
│ N° | Désignation | Prix | Stock                             │
│ ----------------------------------------------------------- │
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

### Règle du stock

Lors de la création d'un médicament :

``` text
stock = 0
```

Le champ stock ne doit donc pas être librement initialisé à une autre
valeur lors de la création.

------------------------------------------------------------------------

## 8.2 Recherche de médicament

La recherche doit se faire sur la désignation.

Le projet demande une recherche utilisant :

``` sql
LIKE '%...%'
```

Source du projet : recherche d'un médicament par désignation avec
`LIKE % …%`. fileciteturn0file0L13-L15

Exemple :

``` text
Recherche : [ para ]

Résultat :
Paracétamol
Paracétamol 500 mg
```

------------------------------------------------------------------------

# 9. STOCK → ENTRÉES DE STOCK

## 9.1 Objectif

Cette partie permet d'enregistrer les entrées de médicaments.

## 9.2 Formulaire

``` text
┌─────────────────────────────────────────────────────────────┐
│ NOUVELLE ENTRÉE DE STOCK                                    │
├─────────────────────────────────────────────────────────────┤
│ Numéro entrée : [____________]                              │
│ Médicament :    [Sélectionner ▼]                            │
│ Quantité :      [____________]                              │
│ Date :          [____________]                              │
│                                                             │
│                  [ ENREGISTRER ]                            │
└─────────────────────────────────────────────────────────────┘
```

## 9.3 Mise à jour du stock

Lorsqu'une entrée est validée :

``` text
stock actuel = stock actuel + stockEntree
```

Exemple :

``` text
Stock actuel : 10
Entrée       : 20
Nouveau stock: 30
```

## 9.4 Liste des entrées

Afficher :

  N° entrée   Médicament      Quantité Date
  ----------- ------------- ---------- ------------
  E001        Paracétamol           20 10/08/2026

Les opérations CRUD doivent être disponibles sur la table ENTREE
conformément au projet. fileciteturn0file0L12-L14

------------------------------------------------------------------------

# 10. MENU ACHAT

Le menu ACHAT doit permettre de réaliser une vente complète.

Organisation :

``` text
ACHAT
├── Nouvelle vente
├── Historique des achats
└── Facture
```

------------------------------------------------------------------------

# 11. ACHAT → NOUVELLE VENTE

## 11.1 Principe

Un client peut acheter plusieurs médicaments lors d'un même achat.

L'écran doit donc permettre de construire un panier avant validation.

## 11.2 Interface recommandée

``` text
┌─────────────────────────────────────────────────────────────┐
│ NOUVEL ACHAT                                                │
├─────────────────────────────────────────────────────────────┤
│ N° Achat :      [____________]                             │
│ Nom client :    [____________]                             │
│ Date :          [____________]                             │
├─────────────────────────────────────────────────────────────┤
│ Médicament :    [Sélectionner ▼]                           │
│ Quantité :      [____]                                     │
│                                                             │
│                 [ AJOUTER ]                                 │
├─────────────────────────────────────────────────────────────┤
│ PANIER                                                      │
│                                                             │
│ Médicament | Prix unitaire | Nombre | Total                 │
│ ----------------------------------------------------------- │
│ Paracétamol| 1000          | 2      | 2000                 │
│ Vitamine C | 500           | 3      | 1500                 │
│                                                             │
│ TOTAL : 3500 Ar                                             │
│                                                             │
│ [Supprimer ligne]        [VALIDER ACHAT]                   │
└─────────────────────────────────────────────────────────────┘
```

## 11.3 Contrôle du stock

Avant de valider une ligne :

``` text
quantité demandée <= stock disponible
```

Si le stock est insuffisant, l'achat doit être refusé et l'application
doit afficher un message clair.

Cette règle est explicitement demandée dans le projet.
fileciteturn0file0L9-L11

Exemple :

``` text
Stock disponible : 3
Quantité demandée : 5

ERREUR :
Stock insuffisant pour Paracétamol.
Stock disponible : 3.
```

## 11.4 Déduction du stock

Après validation :

``` text
nouveau stock = ancien stock - quantité achetée
```

Exemple :

``` text
Stock avant achat : 20
Quantité achetée   : 4
Stock après achat  : 16
```

------------------------------------------------------------------------

# 12. ACHAT → HISTORIQUE

Cette fenêtre affiche les achats déjà enregistrés.

Organisation :

``` text
┌─────────────────────────────────────────────────────────────┐
│ HISTORIQUE DES ACHATS                                      │
├─────────────────────────────────────────────────────────────┤
│ Recherche client : [____________] [Rechercher]              │
├─────────────────────────────────────────────────────────────┤
│ N° Achat | Client | Médicament | Nombre | Date             │
│ ----------------------------------------------------------- │
│ A001     | RAKOTO | Paracétamol| 2      | 23/05/2023       │
│ A002     | RASOA  | Vitamine C | 3      | 24/05/2023       │
└─────────────────────────────────────────────────────────────┘
```

Les opérations CRUD sur la table ACHAT doivent être prévues conformément
au cahier des charges initial. fileciteturn0file0L12-L14

------------------------------------------------------------------------

# 13. FACTURE PDF

Après un achat, l'application doit pouvoir générer une facture au format
PDF.

Le projet demande explicitement la génération d'une facture PDF après
achat. fileciteturn0file0L14-L16

## 13.1 Informations de la facture

La facture doit contenir :

-   date ;
-   nom du client ;
-   désignation du médicament ;
-   prix unitaire ;
-   quantité ;
-   total par ligne ;
-   total général.

## 13.2 Exemple fourni dans le projet

Le modèle du document contient notamment :

``` text
Date : 23/05/2023
Nom du Client : RAKOTO Bernard

Désignation       Prix Unitaire    Nombre    Total

Paracétamol       1000             2         2000 Ar
Vitamine C        500              3         1500 Ar

TOTAL : 3500 Ar
```

Cet exemple doit servir de base pour la mise en page de la facture.
fileciteturn0file0L20-L26

## 13.3 Bouton

Après validation :

``` text
[ Générer facture PDF ]
```

Le bouton doit permettre de créer le fichier PDF de la facture.

------------------------------------------------------------------------

# 14. MENU BILAN

Le menu BILAN regroupe les statistiques et indicateurs de la pharmacie.

Organisation :

``` text
BILAN
├── Recette totale
├── Top 5 médicaments
├── Stock faible
└── Recettes mensuelles
```

------------------------------------------------------------------------

# 15. BILAN → RECETTE TOTALE

Afficher la recette totale accumulée par la pharmacie.

Le projet demande explicitement ce calcul. fileciteturn0file0L16-L18

Exemple :

``` text
RECETTE TOTALE

3 500 Ar
```

Le calcul doit être basé sur les ventes enregistrées.

Pour chaque ligne :

``` text
total ligne = prix_unitaire × quantité
```

Puis :

``` text
recette totale = somme de tous les totaux
```

------------------------------------------------------------------------

# 16. BILAN → TOP 5 DES MÉDICAMENTS

Afficher les cinq médicaments ayant les plus grandes quantités vendues.

Exemple :

    Rang Médicament       Quantité vendue
  ------ -------------- -----------------
       1 Paracétamol                  150
       2 Vitamine C                   120
       3 Amoxicilline                 100
       4 Doliprane                     80
       5 Sirop                         70

Cette fonctionnalité est explicitement demandée par le document.
fileciteturn0file0L17-L19

------------------------------------------------------------------------

# 17. BILAN → STOCK FAIBLE

Afficher les médicaments dont la quantité est inférieure à 5.

Exemple :

  Médicament      Stock État
  ------------- ------- --------------
  Paracétamol         4 Stock faible
  Vitamine C          2 Stock faible
  Doliprane           0 Rupture

Le seuil demandé par le projet est :

``` text
stock < 5
```

------------------------------------------------------------------------

# 18. BILAN → RECETTES DES 5 DERNIERS MOIS

L'application doit afficher un histogramme représentant les recettes des
**5 derniers mois**.

Cette fonctionnalité est explicitement demandée dans le document de
projet. fileciteturn0file0L18-L19

Exemple conceptuel :

``` text
Recette
  ^
  |
  |                 █
  |       █         █
  | █     █   █     █
  | █  █  █   █  █  █
  +------------------------> Mois
    Avril Mai Juin Juil Août
```

## 18.1 Implémentation Swing

Comme l'interface doit rester en Java Swing, le graphique devra être
intégré dans un `JPanel`.

Deux approches sont possibles :

1.  dessiner l'histogramme avec `Graphics2D` dans un `JPanel` ;
2.  utiliser une bibliothèque Java de graphique compatible avec Swing,
    si les dépendances externes sont autorisées.

Pour respecter strictement la contrainte « Java Swing seulement »,
l'option recommandée est un panneau personnalisé utilisant `Graphics2D`.

------------------------------------------------------------------------

# 19. RÈGLES MÉTIER

## 19.1 Création d'un médicament

Lors de la création :

``` text
stock = 0
```

## 19.2 Entrée de stock

Lors d'une entrée :

``` text
stock = stock + quantité entrée
```

## 19.3 Achat

Lors d'un achat :

``` text
stock = stock - quantité achetée
```

## 19.4 Stock insuffisant

Un achat ne doit jamais rendre le stock négatif.

Condition :

``` text
si quantité_achetée > stock
    afficher "Stock insuffisant"
    annuler la validation
sinon
    effectuer l'achat
```

## 19.5 Stock faible

``` text
si stock < 5
    afficher une alerte
```

## 19.6 Total d'une vente

``` text
total = prix_unitaire × nombre
```

## 19.7 Recette

``` text
recette = somme(total de toutes les ventes)
```

------------------------------------------------------------------------

# 20. CRUD À IMPLÉMENTER

Le CRUD doit être disponible sur les trois tables.

## MEDICAMENT

-   Ajouter
-   Afficher
-   Modifier
-   Supprimer

## ENTREE

-   Ajouter
-   Afficher
-   Modifier
-   Supprimer

## ACHAT

-   Ajouter
-   Afficher
-   Modifier
-   Supprimer

Le document source attribue explicitement les opérations CRUD aux trois
tables. fileciteturn0file0L12-L14

------------------------------------------------------------------------

# 21. ORGANISATION DU PROJET DANS VS CODE

Une organisation Java recommandée :

``` text
pharmacie/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── pharmacie/
│   │           │
│   │           ├── Main.java
│   │           │
│   │           ├── model/
│   │           │   ├── Medicament.java
│   │           │   ├── Entree.java
│   │           │   └── Achat.java
│   │           │
│   │           ├── dao/
│   │           │   ├── MedicamentDAO.java
│   │           │   ├── EntreeDAO.java
│   │           │   └── AchatDAO.java
│   │           │
│   │           ├── service/
│   │           │   ├── StockService.java
│   │           │   ├── AchatService.java
│   │           │   └── BilanService.java
│   │           │
│   │           ├── ui/
│   │           │   ├── LoginFrame.java
│   │           │   ├── MainFrame.java
│   │           │   ├── DashboardPanel.java
│   │           │   ├── StockPanel.java
│   │           │   ├── MedicamentPanel.java
│   │           │   ├── EntreePanel.java
│   │           │   ├── AchatPanel.java
│   │           │   └── BilanPanel.java
│   │           │
│   │           └── util/
│   │               ├── DatabaseConnection.java
│   │               ├── PdfGenerator.java
│   │               └── DateUtil.java
│   │
│   └── resources/
│       └── images/
│
├── lib/
├── README.md
└── pom.xml
```

> Si Maven n'est pas utilisé, le projet peut également être configuré
> avec les bibliothèques Java nécessaires directement dans VS Code.

------------------------------------------------------------------------

# 22. RESPONSABILITÉ DES CLASSES

## `Main.java`

Point d'entrée :

``` text
main()
   ↓
ouvrir LoginFrame
```

## `LoginFrame.java`

Responsable :

-   formulaire de connexion ;
-   validation ;
-   ouverture de `MainFrame`.

## `MainFrame.java`

Responsable :

-   fenêtre principale ;
-   menu de navigation ;
-   zone centrale ;
-   déconnexion.

## `DashboardPanel.java`

Responsable :

-   statistiques principales ;
-   alertes ;
-   top 5 ;
-   résumé de la pharmacie.

## `MedicamentPanel.java`

Responsable :

-   CRUD médicaments ;
-   recherche ;
-   affichage du stock.

## `EntreePanel.java`

Responsable :

-   ajout d'entrées ;
-   modification ;
-   suppression ;
-   mise à jour du stock.

## `AchatPanel.java`

Responsable :

-   création d'un achat ;
-   ajout de plusieurs médicaments ;
-   contrôle du stock ;
-   calcul du total ;
-   validation ;
-   génération de facture.

## `BilanPanel.java`

Responsable :

-   recette totale ;
-   top 5 ;
-   stock faible ;
-   histogramme des cinq derniers mois.

------------------------------------------------------------------------

# 23. NAVIGATION

La navigation recommandée est :

``` text
                         LOGIN
                           │
                           ▼
                    MAIN FRAME
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
    DASHBOARD            STOCK              ACHAT
        │                  │                  │
        │            ┌─────┴─────┐       ┌────┴────┐
        │            ▼           ▼       ▼         ▼
        │       MÉDICAMENTS   ENTRÉES  NOUVEL    HISTORIQUE
        │                              ACHAT
        │                                │
        │                                ▼
        │                             FACTURE
        │
        └──────────────────┬─────────────────────
                           ▼
                         BILAN
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
          RECETTE        TOP 5      HISTOGRAMME
```

------------------------------------------------------------------------

# 24. INTERFACE VISUELLE

## 24.1 Style général

L'interface devra être :

-   moderne ;
-   nette ;
-   aérée ;
-   cohérente ;
-   facile à comprendre ;
-   adaptée à une application desktop.

## 24.2 Navigation latérale

Il est recommandé d'utiliser un menu latéral gauche avec :

``` text
┌──────────────────┐
│   PHARMACIE      │
├──────────────────┤
│ 🏠 Dashboard     │
│                  │
│ 📦 Stock         │
│                  │
│ 🛒 Achat         │
│                  │
│ 📊 Bilan         │
│                  │
│ 🚪 Déconnexion   │
└──────────────────┘
```

Les icônes sont optionnelles ; si elles sont utilisées, elles doivent
rester compatibles avec une application Swing.

## 24.3 Tableaux

Les tableaux devront utiliser `JTable` avec :

-   en-têtes clairement visibles ;
-   largeur adaptée des colonnes ;
-   sélection d'une ligne ;
-   actualisation après ajout/modification/suppression ;
-   boutons d'action clairement séparés.

------------------------------------------------------------------------

# 25. MESSAGES UTILISATEUR

L'application devra afficher des messages explicites.

### Succès

``` text
Médicament ajouté avec succès.
```

### Modification

``` text
Médicament modifié avec succès.
```

### Suppression

``` text
Voulez-vous vraiment supprimer ce médicament ?
```

### Stock insuffisant

``` text
Stock insuffisant.
Quantité disponible : 3.
```

### Stock faible

``` text
Attention : ce médicament a un stock inférieur à 5.
```

### Champs obligatoires

``` text
Veuillez remplir tous les champs obligatoires.
```

------------------------------------------------------------------------

# 26. VALIDATIONS

Avant chaque opération, l'application doit vérifier :

-   identifiants non vides ;
-   désignation non vide ;
-   prix valide ;
-   quantité positive ;
-   médicament sélectionné ;
-   date valide ;
-   stock suffisant avant un achat.

Exemple :

``` text
quantité > 0
```

et pour un achat :

``` text
quantité <= stock
```

------------------------------------------------------------------------

# 27. SCÉNARIO COMPLET D'UTILISATION

## Étape 1 --- Connexion

L'utilisateur démarre l'application.

``` text
Login
  ↓
Nom utilisateur
Mot de passe
  ↓
Se connecter
```

## Étape 2 --- Dashboard

Après connexion, le Dashboard est affiché.

L'utilisateur voit :

-   nombre de médicaments ;
-   stocks faibles ;
-   recette totale ;
-   top 5.

## Étape 3 --- Ajouter un médicament

Dans :

``` text
STOCK → MÉDICAMENTS
```

L'utilisateur saisit :

``` text
NumMedoc : M001
Désignation : Paracétamol
Prix : 1000
```

Le stock est automatiquement :

``` text
0
```

## Étape 4 --- Ajouter une entrée

Dans :

``` text
STOCK → ENTRÉES
```

Exemple :

``` text
Médicament : Paracétamol
Quantité : 20
```

Le stock devient :

``` text
20
```

## Étape 5 --- Effectuer un achat

Dans :

``` text
ACHAT → NOUVELLE VENTE
```

Client :

``` text
RAKOTO Bernard
```

Ajout :

``` text
Paracétamol
Quantité : 2
Prix : 1000
Total : 2000 Ar
```

Puis :

``` text
VALIDER ACHAT
```

Le stock devient :

``` text
20 - 2 = 18
```

## Étape 6 --- Générer la facture

Après validation :

``` text
GÉNÉRER FACTURE PDF
```

La facture contient les informations de l'achat.

## Étape 7 --- Consulter le bilan

Dans :

``` text
BILAN
```

L'utilisateur peut consulter :

-   recette totale ;
-   top 5 des médicaments vendus ;
-   médicaments avec stock inférieur à 5 ;
-   recettes des cinq derniers mois.

------------------------------------------------------------------------

# 28. RÉSUMÉ DES EXIGENCES FONCTIONNELLES

  Fonctionnalité                        Obligatoire
  ------------------------------------- -------------
  Login                                 Oui
  Dashboard                             Oui
  Menu STOCK                            Oui
  Gestion MEDICAMENT                    Oui
  Gestion ENTREE                        Oui
  Menu ACHAT                            Oui
  Achat de plusieurs médicaments        Oui
  Contrôle du stock                     Oui
  Alerte stock insuffisant              Oui
  Menu BILAN                            Oui
  CRUD des 3 tables                     Oui
  Recherche par désignation avec LIKE   Oui
  Facture PDF                           Oui
  Liste stock \< 5                      Oui
  Recette totale                        Oui
  Top 5 médicaments vendus              Oui
  Histogramme des 5 derniers mois       Oui
  Java Swing                            Oui
  Développement sous VS Code            Oui

------------------------------------------------------------------------

# 29. CRITÈRES DE VALIDATION DU PROJET

L'application sera considérée comme fonctionnelle si :

1.  le Login fonctionne ;
2.  la fenêtre principale s'ouvre après connexion ;
3.  les quatre espaces Dashboard, Stock, Achat et Bilan sont accessibles
    ;
4.  les trois tables peuvent être gérées avec les opérations CRUD ;
5.  le stock commence à 0 pour un nouveau médicament ;
6.  les entrées augmentent correctement le stock ;
7.  les achats diminuent correctement le stock ;
8.  un achat impossible à cause d'un stock insuffisant est bloqué ;
9.  la recherche par désignation fonctionne avec une recherche partielle
    ;
10. une facture PDF peut être générée après achat ;
11. les médicaments dont le stock est inférieur à 5 sont signalés ;
12. la recette totale est calculée ;
13. les cinq médicaments les plus vendus sont affichés ;
14. l'histogramme des recettes des cinq derniers mois est affiché ;
15. l'interface reste entièrement basée sur Java Swing.

------------------------------------------------------------------------

# 30. Conclusion

Le projet consiste à réaliser une application desktop complète de
**gestion d'une pharmacie**, avec une interface **Java Swing** claire et
organisée.

La structure finale doit être centrée sur :

``` text
LOGIN
  ↓
DASHBOARD
  ↓
┌────────────┬────────────┬────────────┐
│   STOCK    │   ACHAT    │   BILAN    │
└────────────┴────────────┴────────────┘
```

L'application doit couvrir la gestion des médicaments, des entrées de
stock et des achats, tout en fournissant les indicateurs demandés dans
le sujet : stocks faibles, recette totale, top 5 des médicaments vendus
et histogramme des recettes des cinq derniers mois.

**Technologie d'interface : Java Swing uniquement.**\
**Environnement de développement : Visual Studio Code.**
