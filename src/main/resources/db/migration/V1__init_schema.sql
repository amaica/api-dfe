-- Schema inicial (Flyway) baseado nas entidades JPA.
-- Fonte: br.com.synki.apidfe.entity.Empresa / NotaEntrada

CREATE TABLE IF NOT EXISTS empresa_dfe (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cpf_cnpj VARCHAR(32) NOT NULL,
  razao_social VARCHAR(255) NULL,
  uf VARCHAR(2) NULL,
  ambiente ENUM('homologacao','producao') NULL,
  certificado LONGBLOB NOT NULL,
  senha_certificado VARCHAR(255) NULL,
  nsu VARCHAR(32) NULL,
  TIPO_PESSOA VARCHAR(32) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nota_entrada (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chave VARCHAR(60) NOT NULL,
  cnpj_emitente VARCHAR(20) NOT NULL,
  valor DECIMAL(18,2) NOT NULL DEFAULT 0,
  importada VARCHAR(1) NOT NULL DEFAULT 'N',
  nfe_schema VARCHAR(40) NOT NULL DEFAULT 'procNFe_v4.00.xsd',
  cfop VARCHAR(10) NOT NULL DEFAULT '0000',
  natureza VARCHAR(255) NULL,
  ie VARCHAR(30) NULL,
  serie_emitente VARCHAR(10) NULL,
  xml BLOB NULL,
  xml_text TEXT NULL,
  empresa_id BIGINT NULL,
  data_nota VARCHAR(32) NULL,
  data_nota2 DATETIME NULL,
  cod_uf VARCHAR(5) NULL,
  nome_emitente VARCHAR(255) NULL,
  num_nota VARCHAR(20) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_nota_entrada_chave UNIQUE (chave),
  CONSTRAINT fk_nota_entrada_empresa FOREIGN KEY (empresa_id) REFERENCES empresa_dfe(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

