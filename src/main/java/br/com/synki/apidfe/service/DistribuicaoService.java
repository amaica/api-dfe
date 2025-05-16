package br.com.synki.apidfe.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.springframework.stereotype.Service;

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
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNfeProc;
import br.com.swconsultoria.nfe.util.ManifestacaoUtil;
import br.com.swconsultoria.nfe.util.ObjetoUtil;
import br.com.swconsultoria.nfe.util.XmlNfeUtil;
import br.com.synki.apidfe.entity.Empresa;
import br.com.synki.apidfe.entity.NotaEntrada;
import br.com.synki.apidfe.exception.SistemaException;
import br.com.synki.apidfe.repository.EmpresaRepository;
import br.com.synki.apidfe.util.ArquivoUtil;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class DistribuicaoService {

	private final EmpresaRepository empresaRepository;
	private final NotaEntradaService notaEntradaService;

	public DistribuicaoService(EmpresaRepository empresaRepository, NotaEntradaService notaEntradaService) {
		this.empresaRepository = empresaRepository;
		this.notaEntradaService = notaEntradaService;
	}

	public void consultaNotas() throws CertificadoException, NfeException, IOException, JAXBException {
	    List<Empresa> empresas = this.empresaRepository.findAll();
	    for (Empresa empresa : empresas) {
	        if (empresa.getTipoPessoa().equals("F") || empresa.getTipoPessoa().equals("J")) {
	            efetuaConsulta(empresa, empresa.getTipoPessoa().equals("J") ? PessoaEnum.JURIDICA : PessoaEnum.FISICA);
	        }
	    }
	}

	private void efetuaConsulta(Empresa empresa, PessoaEnum tipoPessoa) throws CertificadoException, NfeException, IOException, JAXBException {
	    try {
	        ConfiguracoesNfe configuracao = criaConfiguracao(empresa);
	        List<String> listaNotasManifestar = new ArrayList<>();
	        List<NotaEntrada> listasNotasSalvar = new ArrayList<>();
	        boolean existeMais = true;

	        while (existeMais) {
	            RetDistDFeInt retorno = Nfe.distribuicaoDfe(configuracao, tipoPessoa, empresa.getCpfCnpj(),
	                    ConsultaDFeEnum.NSU, ObjetoUtil.verifica(empresa.getNsu()).orElse("000000000000000"));

	            if (!retorno.getCStat().equals(StatusEnum.DOC_LOCALIZADO_PARA_DESTINATARIO.getCodigo())) {
	                if (retorno.getCStat().equals(StatusEnum.CONSUMO_INDEVIDO.getCodigo())) {
	                    break;
	                } else {
	                    throw new SistemaException(
	                            "Erro ao pesquisar notas: " + retorno.getCStat() + " - " + retorno.getXMotivo());
	                }
	            }

	            populaLista(empresa, listaNotasManifestar, listasNotasSalvar, retorno);
	            existeMais = !retorno.getUltNSU().equals(retorno.getMaxNSU());
	            empresa.setNsu(retorno.getUltNSU());
	        }

	        this.empresaRepository.save(empresa);
	        this.notaEntradaService.salvar(listasNotasSalvar);
	        manifestaListaNotas(listaNotasManifestar, empresa, configuracao);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	  private void populaLista(Empresa empresa, List<String> listaNotasManifestar, List<NotaEntrada> listasNotasSalvar, RetDistDFeInt retorno) throws IOException, JAXBException {
		    for (RetDistDFeInt.LoteDistDFeInt.DocZip doc : retorno.getLoteDistDFeInt().getDocZip()) {
		      ResNFe resNFe;
		      String chave;
		      TNfeProc nfe;
		      NotaEntrada notaEntrada;
		      String xml = XmlNfeUtil.gZipToXml(doc.getValue());
		      System.out.println("Xml: " + xml);
		      System.out.println("Schema: " + doc.getSchema());
		      System.out.println("Nsu: " + doc.getNSU());
		      switch (doc.getSchema()) {
		        case "resNFe_v1.01.xsd":
		          resNFe = (ResNFe)XmlNfeUtil.xmlToObject(xml, ResNFe.class);
		          chave = resNFe.getChNFe();
		          listaNotasManifestar.add(chave);
		        case "procNFe_v4.00.xsd":
		          nfe = (TNfeProc)XmlNfeUtil.xmlToObject(xml, TNfeProc.class);
		          notaEntrada = new NotaEntrada();
		          notaEntrada.setChave(nfe.getNFe().getInfNFe().getId().substring(3));
		          notaEntrada.setEmpresa(empresa);
		          notaEntrada.setSchema(doc.getSchema());
		          notaEntrada.setCfop(((TNFe.InfNFe.Det)nfe.getNFe().getInfNFe().getDet().get(0)).getProd().getCFOP());
		          if (nfe.getNFe().getInfNFe().getEmit().getCNPJ() == null) {
		            notaEntrada.setCnpjEmitente(nfe.getNFe().getInfNFe().getEmit().getCPF());
		          } else {
		            notaEntrada.setCnpjEmitente(nfe.getNFe().getInfNFe().getEmit().getCNPJ());
		          } 
		          if (nfe.getNFe().getInfNFe().getIde().getNNF() != null)
		            notaEntrada.setNumNota(nfe.getNFe().getInfNFe().getIde().getNNF()); 
		          if (nfe.getNFe().getInfNFe().getIde().getNatOp() != null)
		            notaEntrada.setNatureza(nfe.getNFe().getInfNFe().getIde().getNatOp()); 
		          if (nfe.getNFe().getInfNFe().getIde().getDhEmi() != null) {
		            notaEntrada.setDataNota(nfe.getNFe().getInfNFe().getIde().getDhEmi());
		            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
		            String caraceteres = nfe.getNFe().getInfNFe().getIde().getDhEmi().substring(0, 10);
		            try {
		              Date dataN = formato.parse(caraceteres);
		              notaEntrada.setDataNota2(dataN);
		            } catch (ParseException e) {
		              e.printStackTrace();
		            } 
		          } 
		          notaEntrada.setIe(nfe.getNFe().getInfNFe().getEmit().getIE());
		          notaEntrada.setSerieEmitente(nfe.getNFe().getInfNFe().getIde().getSerie());
		          notaEntrada.setCodUf(nfe.getNFe().getInfNFe().getIde().getCUF());
		          notaEntrada.setImportada("N");
		          notaEntrada.setNomeEmitente(nfe.getNFe().getInfNFe().getEmit().getXNome());
		          notaEntrada.setValor(new BigDecimal(nfe.getNFe().getInfNFe().getTotal().getICMSTot().getVNF()));
		          notaEntrada.setXml(ArquivoUtil.compactaXml(xml));
		          listasNotasSalvar.add(notaEntrada);
		      } 
		    } 
		  }


	private void manifestaListaNotas(List<String> chaves, Empresa empresa, ConfiguracoesNfe configuracoesNfe)
			throws NfeException {

		for (String chave : chaves) {
			Evento manifesta = new Evento();
			manifesta.setChave(chave);
			manifesta.setCnpj(empresa.getCpfCnpj());
			manifesta.setMotivo("Manifestacao notas Resumo");
			manifesta.setDataEvento(LocalDateTime.now());
			manifesta.setTipoManifestacao(ManifestacaoEnum.CIENCIA_DA_OPERACAO);

			// Monta o Evento de Manifestação
			TEnvEvento enviEvento = ManifestacaoUtil.montaManifestacao(manifesta, configuracoesNfe);
			// Envia o Evento de Manifestação
			TRetEnvEvento retorno = Nfe.manifestacao(configuracoesNfe, enviEvento, false);
			if (!retorno.getRetEvento().get(0).getInfEvento().getCStat()
					.equals(StatusEnum.EVENTO_VINCULADO.getCodigo())) {
				System.out.println(
						"Erro ao Manufestar Chave " + chave + ": " + retorno.getCStat() + " - " + retorno.getXMotivo());
			}
		}

	}

	private ConfiguracoesNfe criaConfiguracao(Empresa empresa) throws CertificadoException {
		Certificado certificado = CertificadoService.certificadoPfxBytes(empresa.getCertificado(),
				empresa.getSenhaCertificado());
		return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.valueOf(empresa.getUf()), empresa.getAmbiente(),
				certificado, "/d/teste/nfe/schemas");
	}
}
