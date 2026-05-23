package br.com.synki.apidfe.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.Evento;
import br.com.swconsultoria.nfe.dom.enuns.ConsultaDFeEnum;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import br.com.swconsultoria.nfe.dom.enuns.ManifestacaoEnum;
import br.com.swconsultoria.nfe.dom.enuns.PessoaEnum;
import br.com.swconsultoria.nfe.dom.enuns.StatusEnum;
import br.com.swconsultoria.nfe.exception.NfeException;
import br.com.swconsultoria.nfe.schema.envConfRecebto.TEnvEvento;
import br.com.swconsultoria.nfe.schema.envConfRecebto.TRetEnvEvento;
import br.com.swconsultoria.nfe.schema.resnfe.ResNFe;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNfeProc;
import br.com.swconsultoria.nfe.util.ManifestacaoUtil;
import br.com.swconsultoria.nfe.util.ObjetoUtil;
import br.com.swconsultoria.nfe.util.XmlNfeUtil;
import br.com.synki.apidfe.entity.Empresa;
import br.com.synki.apidfe.entity.NotaEntrada;
import br.com.synki.apidfe.exception.SistemaException;
import br.com.synki.apidfe.repository.EmpresaRepository;
import br.com.synki.apidfe.util.ArquivoUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class DistribuicaoService {

	// === Logger funcional (mantendo seus log.* existentes) ===
	private static final Logger log = LoggerFactory.getLogger(DistribuicaoService.class);

	private final EmpresaRepository empresaRepository;
	private final NotaEntradaService notaEntradaService;

	// === Recursos opcionais para fallback nativo (usarão só se presentes/configurados) ===
	@PersistenceContext
	private EntityManager em;

	@Autowired(required = false)
	private JdbcTemplate jdbc;

	public DistribuicaoService(EmpresaRepository empresaRepository, NotaEntradaService notaEntradaService) {
		this.empresaRepository = empresaRepository;
		this.notaEntradaService = notaEntradaService;
	}

	@Transactional(noRollbackFor = Exception.class)
	public void consultaNotas() throws CertificadoException, NfeException, IOException, JAXBException {
		List<Empresa> empresas = this.empresaRepository.findAll();
		for (Empresa empresa : empresas) {
			try {
				if (empresa.getTipoPessoa().equals("F") || empresa.getTipoPessoa().equals("J")) {
					efetuaConsulta(empresa, empresa.getTipoPessoa().equals("J") ? PessoaEnum.JURIDICA : PessoaEnum.FISICA);
				}
			} catch (Exception e) {
				log.error("Falha ao consultar empresa {}: {}", empresa.getCpfCnpj(), e.getMessage(), e);
				// continua para próxima empresa
			}
		}
	}

	@Transactional(noRollbackFor = Exception.class)
	private void efetuaConsulta(Empresa empresa, PessoaEnum tipoPessoa)
			throws CertificadoException, NfeException, IOException, JAXBException {
		try {
			ConfiguracoesNfe configuracao = criaConfiguracao(empresa);
			List<String> listaNotasManifestar = new ArrayList<>();
			List<NotaEntrada> listasNotasSalvar = new ArrayList<>();

			boolean existeMais = true;
			String nsuAtual = leftPadNsu(ObjetoUtil.verifica(empresa.getNsu()).orElse("0"));

			while (existeMais) {
				RetDistDFeInt retorno = null;
				try {
					retorno = Nfe.distribuicaoDfe(configuracao, tipoPessoa, empresa.getCpfCnpj(),
							ConsultaDFeEnum.NSU, nsuAtual);
				} catch (Exception e) {
					log.error("Erro na chamada distribuicaoDfe (nsu={}): {}", nsuAtual, e.getMessage(), e);
					// Para não travar o ciclo, avança NSU artificialmente 1 posição e tenta seguir
					nsuAtual = incrementarNsuSeguro(nsuAtual);
					empresa.setNsu(nsuAtual);
					break; // evita loop infinito; sair e salvar o que já veio
				}

				String cStat = retorno.getCStat();
				String ultNSU = retorno.getUltNSU(); // 15 dígitos
				String maxNSU = retorno.getMaxNSU(); // 15 dígitos

				switch (cStat) {
				case "137": // Nenhum documento localizado
					empresa.setNsu(maxNSU);
					existeMais = false;
					break;

				case "138": // Documentos localizados
					String maiorNSUProcessado = populaLista(empresa, listaNotasManifestar, listasNotasSalvar, retorno);
					String proximo = max(maiorNSUProcessado, ultNSU);
					empresa.setNsu(proximo);
					existeMais = !ultNSU.equals(maxNSU);
					nsuAtual = proximo;
					break;

				case "656": // Consumo Indevido
				case "539": // variações
				default:
					if (StatusEnum.CONSUMO_INDEVIDO.getCodigo().equals(cStat)) {
						log.warn("⚠️ Consumo indevido. cStat={} - {}", cStat, retorno.getXMotivo());
						existeMais = false;
						break;
					}
					throw new SistemaException("Erro ao pesquisar notas: " + cStat + " - " + retorno.getXMotivo());
				}
			}

			// Salva NSU/empresa com tolerância
			salvarEmpresaTolerante(empresa);

			// Tenta salvar em lote; se falhar, cai para nota a nota (com TX isolada e saneamento)
			if (!listasNotasSalvar.isEmpty()) {
				try {
					this.notaEntradaService.salvar(listasNotasSalvar);
				} catch (Exception e) {
					log.error("Falha ao salvar lote de {} notas. Indo nota-a-nota. Erro: {}", listasNotasSalvar.size(), e.getMessage(), e);
					salvarNotasTolerante(listasNotasSalvar);
				}
			}

			// Manifestação já é tolerante a falhas individualmente
			if (!listaNotasManifestar.isEmpty()) {
				try {
					manifestaListaNotas(listaNotasManifestar, empresa, configuracao);
				} catch (Exception e) {
					log.error("Falha geral na manifestação em lote: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			log.error("Exceção geral em efetuaConsulta: {}", e.getMessage(), e);
			// não relança para não gerar rollback; objetivo é salvar o que der
		}
	}

	private static String leftPadNsu(String nsu) {
		try {
			if (nsu == null || nsu.trim().isEmpty())
				return "000000000000000";
			String onlyDigits = nsu.replaceAll("\\D", "");
			if (onlyDigits.length() > 15)
				onlyDigits = onlyDigits.substring(onlyDigits.length() - 15);
			long val = 0L;
			try {
				val = Long.parseLong(onlyDigits.isEmpty() ? "0" : onlyDigits);
			} catch (NumberFormatException e) {
				val = 0L;
			}
			return String.format("%015d", val);
		} catch (Exception e) {
			return "000000000000000";
		}
	}

	// Incrementa NSU com segurança sem estourar tamanho
	private static String incrementarNsuSeguro(String nsu15) {
		try {
			long val = Long.parseLong(nsu15.replaceAll("\\D", ""));
			val = Math.min(val + 1, 999_999_999_999_999L);
			return String.format("%015d", val);
		} catch (Exception e) {
			return "000000000000001";
		}
	}

	// Retorna o MAIOR NSU encontrado no lote (ou o próprio ultNSU se o lote for vazio)
	private static String max(String a, String b) {
		return a.compareTo(b) >= 0 ? a : b;
	}

	private String populaLista(Empresa empresa, List<String> listaNotasManifestar, List<NotaEntrada> listasNotasSalvar,
			RetDistDFeInt retorno) throws IOException, JAXBException {

		String maiorNSU = retorno.getUltNSU(); // fallback
		if (retorno.getLoteDistDFeInt() == null || retorno.getLoteDistDFeInt().getDocZip() == null) {
			return maiorNSU;
		}

		for (RetDistDFeInt.LoteDistDFeInt.DocZip doc : retorno.getLoteDistDFeInt().getDocZip()) {
			if (doc == null || doc.getValue() == null)
				continue;

			String nsu = doc.getNSU();
			if (nsu != null && nsu.length() == 15) {
				maiorNSU = max(maiorNSU, nsu);
			}

			String xml = null;
			try {
				xml = XmlNfeUtil.gZipToXml(doc.getValue());
			} catch (Exception e) {
				log.error("Falha ao descompactar XML NSU {}: {}", nsu, e.getMessage(), e);
				continue;
			}

			System.out.println(
					"Xml: " + (xml != null ? (xml.length() > 120 ? xml.substring(0, 120) + "..." : xml) : "null"));
			System.out.println("Schema: " + doc.getSchema());
			System.out.println("Nsu: " + nsu);

			try {
				switch (doc.getSchema()) {
				case "resNFe_v1.01.xsd": {
					ResNFe resNFe = (ResNFe) XmlNfeUtil.xmlToObject(xml, ResNFe.class);
					if (resNFe != null && resNFe.getChNFe() != null) {
						listaNotasManifestar.add(resNFe.getChNFe());
					}
					break;
				}
				case "procNFe_v4.00.xsd": {
					TNfeProc nfe = (TNfeProc) XmlNfeUtil.xmlToObject(xml, TNfeProc.class);
					if (nfe == null || nfe.getNFe() == null || nfe.getNFe().getInfNFe() == null) {
						System.out.println("⚠️ NFe nula/incompleta. NSU: " + nsu);
						break;
					}

					NotaEntrada notaEntrada = new NotaEntrada();
					try {
						notaEntrada.setChave(nfe.getNFe().getInfNFe().getId().substring(3));
					} catch (Exception e) {
						notaEntrada.setChave("DESCONHECIDA_" + (nsu != null ? nsu : System.currentTimeMillis()));
					}
					notaEntrada.setEmpresa(empresa);
					notaEntrada.setSchema(doc.getSchema()); // propriedade Java continua “schema”

					try {
						notaEntrada.setCfop(nfe.getNFe().getInfNFe().getDet().get(0).getProd().getCFOP());
					} catch (Exception e) {
						notaEntrada.setCfop("0000");
					}

					String cnpj = null;
					try {
						cnpj = nfe.getNFe().getInfNFe().getEmit().getCNPJ();
					} catch (Exception ignore) {}
					if (cnpj == null) {
						try { cnpj = nfe.getNFe().getInfNFe().getEmit().getCPF(); } catch (Exception ignore) {}
					}
					notaEntrada.setCnpjEmitente(cnpj != null ? cnpj : "00000000000000");

					try {
						if (nfe.getNFe().getInfNFe().getIde().getNNF() != null)
							notaEntrada.setNumNota(nfe.getNFe().getInfNFe().getIde().getNNF());
					} catch (Exception ignore) {}

					try {
						if (nfe.getNFe().getInfNFe().getIde().getNatOp() != null)
							notaEntrada.setNatureza(nfe.getNFe().getInfNFe().getIde().getNatOp());
					} catch (Exception ignore) {}

					try {
						if (nfe.getNFe().getInfNFe().getIde().getDhEmi() != null) {
							notaEntrada.setDataNota(nfe.getNFe().getInfNFe().getIde().getDhEmi());
							try {
								String yyyyMMdd = nfe.getNFe().getInfNFe().getIde().getDhEmi().substring(0, 10);
								Date dataN = new SimpleDateFormat("yyyy-MM-dd").parse(yyyyMMdd);
								notaEntrada.setDataNota2(dataN);
							} catch (ParseException ignore) {}
						}
					} catch (Exception ignore) {}

					try {
						notaEntrada.setIe(nfe.getNFe().getInfNFe().getEmit().getIE());
					} catch (Exception ignore) {}

					try {
						notaEntrada.setSerieEmitente(nfe.getNFe().getInfNFe().getIde().getSerie());
					} catch (Exception ignore) {}

					try {
						notaEntrada.setCodUf(nfe.getNFe().getInfNFe().getIde().getCUF());
					} catch (Exception ignore) {}

					notaEntrada.setImportada("N");

					try {
						notaEntrada.setNomeEmitente(nfe.getNFe().getInfNFe().getEmit().getXNome());
					} catch (Exception ignore) {}

					try {
						String vnf = nfe.getNFe().getInfNFe().getTotal().getICMSTot().getVNF();
						notaEntrada.setValor(parseBigDecimalSeguro(vnf));
					} catch (Exception e) {
						notaEntrada.setValor(BigDecimal.ZERO);
					}

					try {
						notaEntrada.setXml(ArquivoUtil.compactaXml(xml));
						notaEntrada.setXmlText(xml);
					} catch (Exception e) {
						log.error("Falha ao compactar/atribuir XML: {}", e.getMessage(), e);
						notaEntrada.setXml(null);
						notaEntrada.setXmlText(xml != null ? (xml.length() > 8000 ? xml.substring(0, 8000) : xml) : null);
					}

					// ---- Hook: saneamento mínimo antes de acumular p/ salvar “a qualquer custo”
					saneiaNota(notaEntrada);

					listasNotasSalvar.add(notaEntrada);
					break;
				}
				default:
					System.out.println("⚠️ Esquema não tratado: " + doc.getSchema());
				}
			} catch (Exception e) {
				log.error("Falha ao processar documento NSU {}: {}", nsu, e.getMessage(), e);
				// segue com os demais
			}
		}
		return maiorNSU;
	}

	private void manifestaListaNotas(List<String> chaves, Empresa empresa, ConfiguracoesNfe configuracoesNfe)
	        throws NfeException {

	    boolean isJuridica = "J".equalsIgnoreCase(empresa.getTipoPessoa());

	    for (String chave : chaves) {
	        try {
	            Evento manifesta = new Evento();
	            manifesta.setChave(chave);
	            if (isJuridica) {
	                manifesta.setCnpj(empresa.getCpfCnpj());
	            } else {
	                manifesta.setCpf(empresa.getCpfCnpj());
	            }
	            manifesta.setMotivo("Manifestacao notas Resumo");
	            manifesta.setDataEvento(LocalDateTime.now());
	            manifesta.setTipoManifestacao(ManifestacaoEnum.CIENCIA_DA_OPERACAO);

	            TEnvEvento enviEvento = ManifestacaoUtil.montaManifestacao(manifesta, configuracoesNfe);
	            TRetEnvEvento retorno = Nfe.manifestacao(configuracoesNfe, enviEvento, false);

	            if (retorno == null || retorno.getRetEvento() == null || retorno.getRetEvento().isEmpty()) {
	                System.out.println("❌ Manifestação sem retorno. Chave: " + chave +
	                        (retorno != null ? " cStat=" + retorno.getCStat() + " - " + retorno.getXMotivo() : ""));
	                continue;
	            }

	            String cStatEvento = retorno.getRetEvento().get(0).getInfEvento().getCStat();
	            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(cStatEvento)) {
	                System.out.println("❌ Falha ao manifestar " + chave + ": " + cStatEvento + " - " +
	                        retorno.getRetEvento().get(0).getInfEvento().getXMotivo());
	            }
	        } catch (Exception e) {
	            System.out.println("❌ Exceção ao manifestar " + chave + ": " + e.getMessage());
	        }
	    }
	}

	private ConfiguracoesNfe criaConfiguracao(Empresa empresa) throws CertificadoException {
		Certificado certificado = CertificadoService.certificadoPfxBytes(empresa.getCertificado(),
				empresa.getSenhaCertificado());
		// Se UF inválida, tenta um fallback para evitar travar
		EstadosEnum uf;
		try {
			uf = EstadosEnum.valueOf(empresa.getUf());
		} catch (Exception e) {
			log.warn("UF inválida '{}' para empresa {}. Usando fallback RS.", empresa.getUf(), empresa.getCpfCnpj());
			uf = EstadosEnum.RS;
		}
		return ConfiguracoesNfe.criarConfiguracoes(uf, empresa.getAmbiente(),
				certificado, "/d/teste/nfe/schemas");
	}

	// ---------- Utilidades de salvamento tolerante ----------

	private void salvarEmpresaTolerante(Empresa empresa) {
		try {
			this.empresaRepository.save(empresa);
		} catch (Exception e) {
			log.error("Falha ao salvar empresa {} (NSU={}): {}", empresa.getCpfCnpj(), empresa.getNsu(), e.getMessage(), e);
		}
	}

	private void salvarNotasTolerante(List<NotaEntrada> notas) {
		for (NotaEntrada n : notas) {
			try {
				// 1) saneamento mínimo
				saneiaNota(n);
				// 2) TX isolada + retry leve
				comRetry(() -> salvarNotaEmTransacaoIsolada(n));
			} catch (Exception e) {
				log.error("Falha ao salvar nota CHAVE={} CNPJ_EMIT={} : {}", n.getChave(), n.getCnpjEmitente(), e.getMessage(), e);
				// 3) Fallback nativo (UPSERT) — só tenta se EM/Jdbc estiverem configurados
				try {
					upsertNativoTolerante(n);
				} catch (Exception ex) {
					log.error("Fallback nativo também falhou para CHAVE={}: {}", n.getChave(), ex.getMessage(), ex);
				}
				// segue adiante; objetivo é salvar o máximo possível
			}
		}
	}

	// Se você preferir realmente garantir commit individual:
	@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
	public void salvarNotaEmTransacaoIsolada(NotaEntrada n) {
		try {
			// tentativa 1: salvar do jeito que está
			this.notaEntradaService.salvar(Collections.singletonList(n));
		} catch (DataIntegrityViolationException e) {
			// violação de NOT NULL/UNIQUE/LENGTH — corrige e tenta de novo
			corrigeParaSalvarMesmoAssim(n, e);
			this.notaEntradaService.salvar(Collections.singletonList(n));
		} catch (Exception e) {
			// erro genérico — corrige e tenta de novo
			corrigeParaSalvarMesmoAssim(n, e);
			this.notaEntradaService.salvar(Collections.singletonList(n));
		}
	}

	private BigDecimal parseBigDecimalSeguro(String valor) {
		try {
			if (valor == null) return BigDecimal.ZERO;
			// Normaliza vírgula para ponto, remove espaços
			String v = valor.trim().replace(",", ".");
			return new BigDecimal(v);
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	// ====== A partir daqui: rotinas auxiliares para “salvar a qualquer custo” ======

	private void saneiaNota(NotaEntrada n) {
		// Preenche mínimos para NOT NULL / defaults
		if (n.getChave() == null || n.getChave().trim().isEmpty()) {
			n.setChave("CHV_" + System.currentTimeMillis());
		}
		if (n.getCnpjEmitente() == null || n.getCnpjEmitente().trim().isEmpty()) {
			n.setCnpjEmitente("00000000000000");
		}
		if (n.getValor() == null) n.setValor(BigDecimal.ZERO);
		if (n.getImportada() == null) n.setImportada("N");
		if (n.getSchema() == null) n.setSchema("procNFe_v4.00.xsd");
		if (n.getCfop() == null) n.setCfop("0000");
		if (n.getNomeEmitente() == null) n.setNomeEmitente("DESCONHECIDO");
		// Corta campos para evitar estourar colunas (ajuste aos tamanhos da sua tabela)
		n.setNatureza(corta(n.getNatureza(), 255));
		n.setIe(corta(n.getIe(), 30));
		n.setSerieEmitente(corta(n.getSerieEmitente(), 10));
		// xmlText pode ser grande — ajuste o limite ao tipo da coluna
		n.setXmlText(corta(n.getXmlText(), 8000));
	}

	private void corrigeParaSalvarMesmoAssim(NotaEntrada n, Exception e) {
		// Regra “grossa”: re-aplica saneamento; se bater UNIQUE(chave), gera variante
		saneiaNota(n);
		if (isUniqueViolation(e)) {
			n.setChave(n.getChave() + "_dup_" + System.currentTimeMillis());
		}
	}

	private boolean isUniqueViolation(Exception e) {
		// Sem depender de classes específicas do driver: heurística por mensagem
		if (e instanceof DataIntegrityViolationException) return true;
		Throwable c = e;
		while (c != null) {
			String msg = c.getMessage();
			if (msg != null) {
				String m = msg.toLowerCase();
				if (m.contains("unique") || m.contains("duplicate") || m.contains("uk_") || m.contains("constraint") || m.contains("violat"))
					return true;
			}
			c = c.getCause();
		}
		return false;
	}

	private String corta(String s, int max) {
		if (s == null) return null;
		return s.length() > max ? s.substring(0, max) : s;
	}

	private void comRetry(Runnable r) {
		int tentativas = 0;
		while (true) {
			try {
				r.run();
				return;
			} catch (Exception e) {
				tentativas++;
				if (tentativas >= 3) throw e;
				try { Thread.sleep(150L * tentativas); } catch (InterruptedException ignored) {}
			}
		}
	}

	/**
	 * Fallback “hardcore”: tenta UPSERT nativo.
	 * - Primeiro tenta Postgres (ON CONFLICT).
	 * - Se falhar, tenta MySQL/MariaDB (ON DUPLICATE KEY).
	 * - Se não houver EntityManager/JdbcTemplate, apenas loga.
	 */
	private void upsertNativoTolerante(NotaEntrada n) {
		try {
			if (em != null) {
				// Tenta padrão Postgres (ajuste nomes de coluna/tabela conforme seu mapeamento real)
				em.createNativeQuery(
					// ALTERAÇÃO: trocar "schema" por "nfe_schema"
					"INSERT INTO nota_entrada (chave, cnpj_emitente, valor, importada, nfe_schema, cfop, natureza, ie, serie_emitente, xml, xml_text, empresa_id, data_nota, data_nota2, cod_uf, nome_emitente) " +
					"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
					"ON CONFLICT (chave) DO UPDATE SET " +
					"valor=EXCLUDED.valor, xml=EXCLUDED.xml, xml_text=EXCLUDED.xml_text, nome_emitente=EXCLUDED.nome_emitente"
				)
				.setParameter(1, n.getChave())
				.setParameter(2, n.getCnpjEmitente())
				.setParameter(3, n.getValor())
				.setParameter(4, n.getImportada())
				.setParameter(5, n.getSchema()) // propriedade Java continua schema; coluna é nfe_schema
				.setParameter(6, n.getCfop())
				.setParameter(7, n.getNatureza())
				.setParameter(8, n.getIe())
				.setParameter(9, n.getSerieEmitente())
				.setParameter(10, n.getXml())
				.setParameter(11, n.getXmlText())
				.setParameter(12, n.getEmpresa() != null ? n.getEmpresa().getId() : null)
				.setParameter(13, n.getDataNota())
				.setParameter(14, n.getDataNota2())
				.setParameter(15, n.getCodUf())
				.setParameter(16, n.getNomeEmitente())
				.executeUpdate();
				return;
			}
		} catch (Exception pgEx) {
			log.warn("UPSERT Postgres falhou (tentará MySQL/MariaDB): {}", pgEx.getMessage());
			// cai para tentativa MySQL/MariaDB abaixo
		}

		try {
			if (jdbc != null) {
				// Tenta MySQL/MariaDB
				// ALTERAÇÃO: trocar "schema" por "nfe_schema"
				String sql =
					"INSERT INTO nota_entrada (chave, cnpj_emitente, valor, importada, nfe_schema, cfop, natureza, ie, serie_emitente, xml, xml_text, empresa_id, data_nota, data_nota2, cod_uf, nome_emitente) " +
					"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
					"ON DUPLICATE KEY UPDATE valor=VALUES(valor), xml=VALUES(xml), xml_text=VALUES(xml_text), nome_emitente=VALUES(nome_emitente)";
				jdbc.update(sql,
					n.getChave(), n.getCnpjEmitente(), n.getValor(), n.getImportada(), n.getSchema(), n.getCfop(),
					n.getNatureza(), n.getIe(), n.getSerieEmitente(), n.getXml(), n.getXmlText(),
					(n.getEmpresa() != null ? n.getEmpresa().getId() : null),
					n.getDataNota(), n.getDataNota2(), n.getCodUf(), n.getNomeEmitente()
				);
				return;
			}
		} catch (Exception myEx) {
			log.warn("UPSERT MySQL/MariaDB falhou: {}", myEx.getMessage());
		}

		log.error("Não foi possível executar UPSERT nativo (sem EM/Jdbc ou ambos falharam) para CHAVE={}", n.getChave());
	}
}
