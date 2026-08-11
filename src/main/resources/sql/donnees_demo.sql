-- =====================================================================
--  JEU DE DONNEES DE DEMONSTRATION
--
--  A executer APRES schema.sql :
--      mysql -u root -p < src/main/resources/sql/donnees_demo.sql
--
--  Les stocks ne sont jamais saisis directement : ils sont calcules a
--  partir des entrees puis des achats, conformement aux regles metier
--  du cahier des charges (stock initial = 0).
-- =====================================================================

USE pharmacie;

-- ---------------------------------------------------------------------
--  Medicaments : creation systematique avec stock = 0
-- ---------------------------------------------------------------------
INSERT INTO medicament (numMedoc, Design, prix_unitaire, stock) VALUES
    ('M001', 'Paracetamol 500 mg',   1000, 0),
    ('M002', 'Vitamine C 1 g',        500, 0),
    ('M003', 'Amoxicilline 500 mg',  2500, 0),
    ('M004', 'Doliprane 1000 mg',    1200, 0),
    ('M005', 'Sirop antitussif',     3500, 0),
    ('M006', 'Ibuprofene 400 mg',    1500, 0),
    ('M007', 'Aspirine 500 mg',       800, 0),
    ('M008', 'Serum physiologique',   600, 0),
    ('M009', 'Pommade cicatrisante', 4000, 0),
    ('M010', 'Collyre apaisant',     2800, 0);

-- ---------------------------------------------------------------------
--  Entrees de stock : chaque entree augmente le stock du medicament
-- ---------------------------------------------------------------------
INSERT INTO entree (numEntree, numMedoc, stockEntree, dateEntree) VALUES
    ('E001', 'M001', 200, DATE_SUB(CURDATE(), INTERVAL 150 DAY)),
    ('E002', 'M002', 150, DATE_SUB(CURDATE(), INTERVAL 150 DAY)),
    ('E003', 'M003', 120, DATE_SUB(CURDATE(), INTERVAL 120 DAY)),
    ('E004', 'M004', 100, DATE_SUB(CURDATE(), INTERVAL 120 DAY)),
    ('E005', 'M005',  90, DATE_SUB(CURDATE(),  INTERVAL 90 DAY)),
    ('E006', 'M006',  80, DATE_SUB(CURDATE(),  INTERVAL 60 DAY)),
    ('E007', 'M007',  60, DATE_SUB(CURDATE(),  INTERVAL 60 DAY)),
    ('E008', 'M008',  12, DATE_SUB(CURDATE(),  INTERVAL 30 DAY)),
    ('E009', 'M009',   8, DATE_SUB(CURDATE(),  INTERVAL 30 DAY)),
    ('E010', 'M010',   6, DATE_SUB(CURDATE(),  INTERVAL 15 DAY)),
    ('E011', 'M001',  50, DATE_SUB(CURDATE(),  INTERVAL 10 DAY));

UPDATE medicament m
SET m.stock = (
    SELECT COALESCE(SUM(e.stockEntree), 0)
    FROM entree e
    WHERE e.numMedoc = m.numMedoc
);

-- ---------------------------------------------------------------------
--  Achats repartis sur les 5 derniers mois (histogramme du bilan).
--  Un meme numAchat regroupe plusieurs medicaments (panier multi-lignes).
-- ---------------------------------------------------------------------
INSERT INTO achat (numAchat, numMedoc, nomClient, nbr, dateAchat, prix_unitaire) VALUES
    ('A001', 'M001', 'RAKOTO Bernard',  40, DATE_SUB(CURDATE(), INTERVAL 4 MONTH), 1000),
    ('A001', 'M002', 'RAKOTO Bernard',  30, DATE_SUB(CURDATE(), INTERVAL 4 MONTH),  500),
    ('A002', 'M003', 'RASOA Marie',     20, DATE_SUB(CURDATE(), INTERVAL 4 MONTH), 2500),
    ('A003', 'M001', 'RANDRIA Paul',    35, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 1000),
    ('A003', 'M004', 'RANDRIA Paul',    25, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 1200),
    ('A004', 'M005', 'RABE Jean',       15, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 3500),
    ('A005', 'M002', 'RASOA Marie',     40, DATE_SUB(CURDATE(), INTERVAL 2 MONTH),  500),
    ('A006', 'M001', 'RAKOTO Bernard',  45, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 1000),
    ('A006', 'M006', 'RAKOTO Bernard',  30, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 1500),
    ('A007', 'M003', 'RAVELO Sophie',   30, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 2500),
    ('A008', 'M004', 'RANDRIA Paul',    28, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 1200),
    ('A009', 'M007', 'RABE Jean',       25, DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  800),
    ('A010', 'M001', 'RAVELO Sophie',   50, CURDATE(),                             1000),
    ('A010', 'M002', 'RAVELO Sophie',   35, CURDATE(),                              500),
    ('A011', 'M008', 'RAKOTO Bernard',  10, CURDATE(),                              600),
    ('A012', 'M009', 'RASOA Marie',      6, CURDATE(),                             4000);

-- Application des achats sur le stock : stock = entrees - achats
UPDATE medicament m
SET m.stock = m.stock - (
    SELECT COALESCE(SUM(a.nbr), 0)
    FROM achat a
    WHERE a.numMedoc = m.numMedoc
);
