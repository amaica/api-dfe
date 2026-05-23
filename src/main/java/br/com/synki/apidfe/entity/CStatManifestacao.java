package br.com.synki.apidfe.entity;

public enum CStatManifestacao {

    SUCESSO("128", "Lote recebido com sucesso"),
    EVENTO_VINCULADO("135", "Evento registrado e vinculado a NFe"),
    EVENTO_DUPLICADO("136", "Evento já registrado para esta NFe"),
    EVENTO_VINCULADO_OUTRO("137", "Evento vinculado à NFe, mas com conteúdo diferente"),
    FALHA_SCHEMA("225", "Falha no Esquema XML do lote de NF-e"),
    CHAVE_INVALIDA("573", "Rejeição: Chave de Acesso inválida"),
    NAO_LOCALIZADA("217", "Rejeição: NF-e não consta na base de dados da SEFAZ");

    private final String codigo;
    private final String mensagem;

    CStatManifestacao(String codigo, String mensagem) {
        this.codigo = codigo;
        this.mensagem = mensagem;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public static String getMensagemPorCodigo(String codigo) {
        for (CStatManifestacao c : values()) {
            if (c.getCodigo().equals(codigo)) {
                return c.getMensagem();
            }
        }
        return "Código desconhecido";
    }
}
