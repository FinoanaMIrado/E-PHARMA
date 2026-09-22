# Gestion de Pharmacie — Application Java Swing

Application desktop de gestion d'une pharmacie : médicaments, entrées de stock,
ventes multi-lignes, facture PDF et bilan avec histogramme.

**Technologies :** Java 17+ · Java Swing · MySQL/MariaDB · JDBC · Maven · Apache PDFBox

---

## 1. Prérequis

| Élément | Version | Vérification |
|---|---|---|
| JDK | 17 ou supérieur | `java -version` |
| Maven | 3.6 ou supérieur | `mvn -version` |
| MySQL ou MariaDB | 5.7+ / 10.3+ | `mysql --version` |

Extension VS Code recommandée : **Extension Pack for Java** (Microsoft).

---

## 2. Installation de la base de données

Depuis la racine du projet :

```bash
# 1. Créer la base, les tables et le compte utilisateur
mysql -u root -p < src/main/resources/sql/schema.sql

# 2. (Facultatif) Charger un jeu de données de démonstration
mysql -u root -p < src/main/resources/sql/donnees_demo.sql
```

Le second script crée 10 médicaments, 11 entrées et 12 achats répartis sur les
5 derniers mois, afin que le tableau de bord et l'histogramme soient immédiatement
peuplés.

### Vérification

```bash
mysql -u root -p -e "USE pharmacie; SHOW TABLES;"
```

Quatre tables doivent apparaître : `achat`, `entree`, `medicament`, `utilisateur`.

---

## 3. Configuration de la connexion

Adaptez `src/main/resources/database.properties` à votre installation :

```properties
db.host=localhost
db.port=3306
db.name=pharmacie
db.user=root
db.password=VOTRE_MOT_DE_PASSE
```

Chaque paramètre peut aussi être surchargé au lancement, sans modifier le fichier :

```bash
mvn exec:java -Ddb.password=secret -Ddb.port=3307
```

---

## 4. Lancement

### Depuis VS Code

Ouvrir le dossier du projet, puis ouvrir `src/main/java/pharmacie/Main.java`
et cliquer sur **Run** au-dessus de la méthode `main`.

### Depuis le terminal

```bash
# Compilation
mvn clean compile

# Lancement
mvn exec:java
```

### JAR autonome

```bash
mvn clean package
java -jar target/gestion-pharmacie.jar
```

Le JAR produit embarque le pilote JDBC, PDFBox et toutes les icônes : il fonctionne
sur n'importe quel poste disposant d'un JDK 17 et d'un accès à la base.

---

## 5. Identifiants de connexion

| Nom d'utilisateur | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `f| Pimiri` | `qwerty1234` HARMACIEN |

Les mots de passe sont stockés hachés (BCrypt) : la base ne contient jamais de mot
de passe en clair.

> **Important — ne jamais insérer un mot de passe en clair dans la table.**
> L'application vérifie les mots de passe avec `BCrypt.verify()`. Une valeur en
> clair saisie directement en base ne peut jamais être validée : la connexion
> échouera **même avec le bon mot de passe**, sans message explicite (le message
> reste volontairement « Nom d'utilisateur ou mot de passe incorrect »).

### Créer un autre utilisateur

Générer un hachage puis l'insérer en base :

```java
System.out.println(pharmacie.service.AuthService.hacher("nouveauMotDePasse"));
```

```sql
INSERT INTO utilisateur (login, motDePasse, nomComplet, role)
VALUES ('pharmacien', '<hachage_genere>', 'Nom Complet', 'PHARMACIEN');
```

### Réparer des mots de passe stockés en clair

Si des comptes ont été créés à la main avec un mot de passe en clair, cet outil
les convertit en hachage BCrypt **sans changer les mots de passe eux-mêmes** —
chaque utilisateur continue de saisir exactement la même chose qu'avant. Les
comptes déjà hachés sont laissés intacts, le script est donc rejouable :

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
CP="target/classes:$(cat cp.txt)"

javac -encoding UTF-8 -cp "$CP" -d /tmp/tools tools/MigrationMotsDePasse.java
java -cp "$CP:/tmp/tools" pharmacie.ui.MigrationMotsDePasse
```

L'outil affiche le résultat compte par compte, puis rejoue une authentification
réelle pour confirmer que chaque connexion fonctionne.

---

## 6. Structure du projet

```text
.
├── pom.xml                                  Dépendances et build Maven
├── README.md
├── cahier_des_charges_pharmacie.md
│
├── src/main/java/pharmacie/
│   ├── Main.java                            Point d'entrée
│   │
│   ├── model/                               Objets métier
│   │   ├── Medicament.java                  (seuil de stock faible = 5)
│   │   ├── Entree.java
│   │   ├── Achat.java
│   │   ├── LigneVente.java                  Ligne du panier (en mémoire)
│   │   ├── Facture.java
│   │   └── Utilisateur.java
│   │
│   ├── dao/                                 Accès aux données (PreparedStatement)
│   │   ├── MedicamentDAO.java
│   │   ├── EntreeDAO.java
│   │   ├── AchatDAO.java
│   │   └── UtilisateurDAO.java
│   │
│   ├── service/                             Règles métier et transactions
│   │   ├── StockService.java
│   │   ├── AchatService.java
│   │   ├── BilanService.java
│   │   ├── AuthService.java
│   │   └── FacturePdfService.java
│   │
│   ├── ui/                                  Interfaces Swing
│   │   ├── Theme.java                       Charte graphique
│   │   ├── LoginFrame.java
│   │   ├── MainFrame.java                   Menu latéral + CardLayout
│   │   ├── DashboardPanel.java
│   │   ├── StockPanel.java                  Onglets Médicaments / Entrées
│   │   ├── MedicamentPanel.java             CRUD + recherche
│   │   ├── EntreePanel.java
│   │   ├── AchatPanel.java                  Onglets Vente / Historique
│   │   ├── NouvelleVentePanel.java          Panier multi-médicaments
│   │   ├── HistoriqueAchatPanel.java
│   │   ├── BilanPanel.java
│   │   └── components/                      Composants réutilisables
│   │       ├── BoutonAction.java
│   │       ├── Carte.java
│   │       ├── CarteIndicateur.java
│   │       ├── Dialogues.java
│   │       ├── HistogrammeRecettes.java     Graphique Graphics2D
│   │       └── UIFactory.java
│   │
│   └── util/
│       ├── AppConfig.java
│       ├── DatabaseConnection.java          Connexion JDBC
│       ├── DateUtil.java
│       ├── MontantUtil.java
│       ├── IconLoader.java
│       └── BusinessException.java
│
├── src/main/resources/
│   ├── database.properties
│   ├── icons/                               24 icônes PNG + 24 en blanc
│   └── sql/
│       ├── schema.sql
│       └── donnees_demo.sql
│
└── tools/                                   Outils de développement (hors application)
    ├── IconGenerator.java                   Génère les icônes PNG
    ├── ServiceSmokeTest.java                Test d'intégration des services
    ├── ScenarioCompletTest.java             Scénario complet du cahier des charges
    ├── TestFacturePdf.java                  Vérifie le contenu de la facture
    ├── MigrationMotsDePasse.java            Hache les mots de passe en clair
    ├── CaptureEcrans.java                   Captures d'écran des 5 vues
    └── FactureDemo.java
```

---

## 7. Règles métier appliquées

| Règle | Implémentation |
|---|---|
| Stock initial = 0 à la création | `StockService.creerMedicament` force `stock = 0` |
| Entrée : `stock = stock + quantité` | `StockService.creerEntree`, en transaction |
| Achat : `stock = stock - quantité` | `AchatService.validerVente`, en transaction |
| Stock jamais négatif | Clause SQL `WHERE stock + ? >= 0` dans `ajusterStock` |
| Stock insuffisant → achat bloqué | Vérifié à l'ajout au panier **et** à la validation |
| Alerte si `stock < 5` | `Medicament.SEUIL_STOCK_FAIBLE` |
| Recherche `LIKE '%texte%'` | `MedicamentDAO.searchByDesign`, paramètre lié |
| Total ligne = prix × quantité | `Achat.getTotalLigne`, `LigneVente.getTotal` |

Le contrôle du stock est effectué **deux fois** : à l'ajout au panier pour prévenir
l'utilisateur immédiatement, puis à la validation à l'intérieur de la transaction,
car le stock a pu changer entre-temps.

---

## 8. Sécurité

- Toutes les requêtes utilisent `PreparedStatement` avec paramètres liés :
  aucune valeur saisie n'est concaténée dans du SQL.
- Les caractères spéciaux `%` et `_` des recherches `LIKE` sont échappés,
  afin qu'une saisie contenant `%` ne retourne pas toute la table.
- Les mots de passe sont hachés avec BCrypt (coût 12).
- Le tableau de caractères du mot de passe est effacé de la mémoire après usage.
- Le message d'échec de connexion ne révèle pas si le login existe.

---

## 9. Décisions documentées

Points non spécifiés par le cahier des charges, tranchés simplement :

1. **Table `utilisateur`** — l'écran de connexion est exigé mais aucune table n'est
   décrite. Une table minimale a été ajoutée, avec hachage BCrypt.

2. **Colonne `achat.prix_unitaire`** — le prix est figé au moment de la vente.
   Sans cela, modifier le prix d'un médicament fausserait rétroactivement les
   factures déjà émises ainsi que la recette totale.

3. **Clé primaire de `achat` = (`numAchat`, `numMedoc`)** — permet à un même achat
   de contenir plusieurs médicaments, comme demandé.

4. **Stock non modifiable à la main** — le formulaire médicament affiche le stock
   en lecture seule ; il n'évolue que par les entrées et les achats.

5. **Suppression protégée** — supprimer un médicament référencé par une entrée ou un
   achat est refusé, pour préserver l'historique.

6. **Format de date `jj/mm/aaaa`** — saisi dans un `JTextField` standard, sans
   bibliothèque externe de calendrier.

7. **Devise « Ar »** — reprise des exemples du cahier des charges, montants entiers.

---

## 10. Icônes

L'interface n'utilise **aucun emoji ni symbole Unicode décoratif** : toutes les
icônes sont des fichiers PNG du projet, chargés via `ImageIcon`.

Elles sont générées par un script Java2D, ce qui garantit un style homogène
(même grille 24×24, même épaisseur de trait) :

```bash
java tools/IconGenerator.java src/main/resources/icons
```

Deux variantes de chaque icône sont produites : sombre pour les fonds clairs,
blanche (`icons/white/`) pour le menu latéral et les pastilles colorées.

---

## 11. Tests

Les outils de `tools/` vérifient l'application contre une base réelle. Compiler
puis exécuter avec le classpath du projet :

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
CP="target/classes:$(cat cp.txt)"

javac -cp "$CP" -d /tmp/tests tools/ScenarioCompletTest.java
java -cp "$CP:/tmp/tests" pharmacie.ui.ScenarioCompletTest
```

`ScenarioCompletTest` rejoue les 12 étapes du scénario du cahier des charges :
connexion, refus d'un mot de passe erroné, création à stock 0, refus des doublons
et des prix invalides, entrée de stock, vente, blocage sur stock insuffisant, achat
multi-médicaments, recherche partielle, protection de l'intégrité, puis nettoyage.

---

## 12. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| « Connexion à la base de données impossible » | Serveur arrêté ou paramètres erronés | Démarrer MySQL, vérifier `database.properties` |
| `Access denied for user` | Mot de passe incorrect | Corriger `db.password` |
| `Unknown database 'pharmacie'` | Schéma non installé | Exécuter `schema.sql` |
| « Nom d'utilisateur ou mot de passe incorrect » | Table `utilisateur` vide | Réexécuter `schema.sql` (il crée les comptes) |
| « Nom d'utilisateur ou mot de passe incorrect » **alors que le mot de passe est le bon** | Mot de passe stocké en clair au lieu d'un hachage BCrypt | Exécuter `MigrationMotsDePasse` (section 5) |
| Le mot de passe semble correct mais reste refusé après migration | Retour chariot ou espace invisible dans la valeur saisie en base | `MigrationMotsDePasse` retire ces caractères ; vérifier avec `SELECT HEX(motDePasse) FROM utilisateur;` |
| Tableau de bord vide | Aucune donnée | Exécuter `donnees_demo.sql` |
