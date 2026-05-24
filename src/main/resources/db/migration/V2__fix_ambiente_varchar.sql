-- Corrige ENUM minúsculo (producao/homologacao) incompatível com AmbienteEnum (PRODUCAO/HOMOLOGACAO)
ALTER TABLE empresa_dfe MODIFY ambiente VARCHAR(32) NULL;

UPDATE empresa_dfe SET ambiente = 'PRODUCAO' WHERE LOWER(ambiente) = 'producao';
UPDATE empresa_dfe SET ambiente = 'HOMOLOGACAO' WHERE LOWER(ambiente) = 'homologacao';
