package br.com.synki.apidfe.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "nota_entrada")
@SequenceGenerator(name = "NotaEntradaSeq", sequenceName = "SEQ_NOTA_ENTRADA", allocationSize = 1)
@Data
public class NotaEntrada {

	@Id
	@GeneratedValue(generator = "NotaEntradaSeq", strategy = GenerationType.SEQUENCE)
	private Long id;
	@Column(name = "caminho_schema")
	private String schema;

	@Column(name = "chave")
	private String chave;

	@Column(name = "cfop")
	private String cfop;

	@Column(name = "numero_nota")
	private String numNota;

	@Column(name = "data_nota")
	private String dataNota;

	@Column(name = "importada")
	private String importada;

	@Column(name = "natureza")
	private String natureza;

	@Column(name = "nome_emitente")
	private String nomeEmitente;

	@Column(name = "cnpj_emitente")
	private String cnpjEmitente;

	@Column(name = "ie_emitente")
	private String ie;

	@Column(name = "serie_emitente")
	private String serieEmitente;

	@Column(name = "cod_uf_emitente")
	private String codUf;

	@Column(name = "DATA_NOTA2")
	private Date dataNota2;

	@Column(name = "valor")
	private BigDecimal valor;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column(name = "xml")
	private byte[] xml;

	@ManyToOne
	@JoinColumn(name = "empresa_id")
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Empresa empresa;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getChave() {
		return chave;
	}

	public void setChave(String chave) {
		this.chave = chave;
	}

	public String getNomeEmitente() {
		return nomeEmitente;
	}

	public void setNomeEmitente(String nomeEmitente) {
		this.nomeEmitente = nomeEmitente;
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

	public byte[] getXml() {
		return xml;
	}

	public void setXml(byte[] xml) {
		this.xml = xml;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getCfop() {
		return cfop;
	}

	public void setCfop(String cfop) {
		this.cfop = cfop;
	}

	public String getNumNota() {
		return numNota;
	}

	public void setNumNota(String numNota) {
		this.numNota = numNota;
	}

	public String getDataNota() {
		return dataNota;
	}

	public void setDataNota(String dataNota) {
		this.dataNota = dataNota;
	}

	public String getImportada() {
		return importada;
	}

	public void setImportada(String importada) {
		this.importada = importada;
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

	public String getCodUf() {
		return codUf;
	}

	public void setCodUf(String codUf) {
		this.codUf = codUf;
	}

	public Date getDataNota2() {
		return dataNota2;
	}

	public void setDataNota2(Date dataNota2) {
		this.dataNota2 = dataNota2;
	}

}
