package br.com.synki.apidfe.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

// br.com.synki.apidfe.entity.NotaEntrada
@Entity
@Table(name = "nota_entrada", uniqueConstraints = {
    @UniqueConstraint(name = "uk_nota_entrada_chave", columnNames = "chave")
})
public class NotaEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false)
    private String chave;

    @Column(name = "cnpj_emitente", length = 20, nullable = false)
    private String cnpjEmitente;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal valor = BigDecimal.ZERO;

    @Column(length = 1, nullable = false)
    private String importada = "N";

    @Column(name = "nfe_schema", length = 40, nullable = false)
    private String schema = "procNFe_v4.00.xsd";

    @Column(length = 10, nullable = false)
    private String cfop = "0000";

    @Column(length = 255)
    private String natureza;

    @Column(length = 30)
    private String ie;

    @Column(name = "serie_emitente", length = 10)
    private String serieEmitente;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] xml; // Postgres BYTEA / MySQL LONGBLOB

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "xml_text", columnDefinition = "TEXT")
    private String xmlText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "data_nota", length = 32)
    private String dataNota;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_nota2")
    private Date dataNota2;

    @Column(name = "cod_uf", length = 5)
    private String codUf;

    @Column(name = "nome_emitente", length = 255)
    private String nomeEmitente;
    @Column(name = "num_nota", length = 20)
    private String numNota;
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getChave() {
		return chave;
	}

	public void setChave(String chave) {
		this.chave = chave;
	}

	public String getCnpjEmitente() {
		return cnpjEmitente;
	}

	public void setCnpjEmitente(String cnpjEmitente) {
		this.cnpjEmitente = cnpjEmitente;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public String getImportada() {
		return importada;
	}

	public void setImportada(String importada) {
		this.importada = importada;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getCfop() {
		return cfop;
	}

	public void setCfop(String cfop) {
		this.cfop = cfop;
	}

	public String getNatureza() {
		return natureza;
	}

	public void setNatureza(String natureza) {
		this.natureza = natureza;
	}

	public String getIe() {
		return ie;
	}

	public void setIe(String ie) {
		this.ie = ie;
	}

	public String getSerieEmitente() {
		return serieEmitente;
	}

	public void setSerieEmitente(String serieEmitente) {
		this.serieEmitente = serieEmitente;
	}

	public byte[] getXml() {
		return xml;
	}

	public void setXml(byte[] xml) {
		this.xml = xml;
	}

	public String getXmlText() {
		return xmlText;
	}

	public void setXmlText(String xmlText) {
		this.xmlText = xmlText;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getDataNota() {
		return dataNota;
	}

	public void setDataNota(String dataNota) {
		this.dataNota = dataNota;
	}

	public Date getDataNota2() {
		return dataNota2;
	}

	public void setDataNota2(Date dataNota2) {
		this.dataNota2 = dataNota2;
	}

	public String getCodUf() {
		return codUf;
	}

	public void setCodUf(String codUf) {
		this.codUf = codUf;
	}

	public String getNomeEmitente() {
		return nomeEmitente;
	}

	public void setNomeEmitente(String nomeEmitente) {
		this.nomeEmitente = nomeEmitente;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NotaEntrada other = (NotaEntrada) obj;
		return Objects.equals(id, other.id);
	}

	public String getNumNota() {
		return numNota;
	}

	public void setNumNota(String numNota) {
		this.numNota = numNota;
	}

    
}
