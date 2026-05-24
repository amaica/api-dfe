package br.com.synki.apidfe.entity;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresa_dfe")

public class Empresa implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Basic(optional = false)
	private Long id;
	private String cpfCnpj;
	private String razaoSocial;
	private String uf;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private AmbienteEnum ambiente;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private byte[] certificado;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String senhaCertificado;
	private String nsu;
	@Column(name = "TIPO_PESSOA")
	private String tipoPessoa;

	public byte[] getCertificado() {
		return certificado;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCpfCnpj() {
		return cpfCnpj;
	}

	public void setCpfCnpj(String cpfCnpj) {
		this.cpfCnpj = cpfCnpj;
	}

	public String getRazaoSocial() {
		return razaoSocial;
	}

	public void setRazaoSocial(String razaoSocial) {
		this.razaoSocial = razaoSocial;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public AmbienteEnum getAmbiente() {
		return ambiente;
	}

	public void setAmbiente(AmbienteEnum ambiente) {
		this.ambiente = ambiente;
	}

	public String getSenhaCertificado() {
		return senhaCertificado;
	}

	public void setSenhaCertificado(String senhaCertificado) {
		this.senhaCertificado = senhaCertificado;
	}

	public String getTipoPessoa() {
		return tipoPessoa;
	}

	public void setTipoPessoa(String tipoPessoa) {
		this.tipoPessoa = tipoPessoa;
	}

	public String getNsu() {
		return nsu;
	}

	public void setNsu(String nsu) {
		this.nsu = nsu;
	}

	public void setCertificado(byte[] certificado) {
		this.certificado = certificado;
	}

}
