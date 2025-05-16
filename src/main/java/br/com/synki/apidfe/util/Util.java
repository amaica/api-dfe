package br.com.synki.apidfe.util;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Samuel OLiveira.
 */

public final class Util {

    private static final String CHARSET_UFT_8 = "UTF-8";

    /**
     * Construtor privado para garantir o Singleton.
     */
    private Util() {

    }

    /**
     * Verifica se um objeto &eacute; vazio.
     *
     * @param obj
     * @return <b>true</b> se o objeto for vazio(empty).
     */
    public static <T> Optional<T> verifica(T obj) {
        if (obj == null)
            return Optional.empty();
        if (obj instanceof Collection)
            return ((Collection<?>) obj).size() == 0 ? Optional.empty() : Optional.of(obj);

        final String s = String.valueOf(obj).trim();

        return s.length() == 0 || s.equalsIgnoreCase("null") ? Optional.empty() : Optional.of(obj);
    }

    public static boolean isNull(final Object valor) {
        return valor == null;
    }


    /**
     * Retorna o proprio valor ou NUll se for String vazia
     *
     * @param valor
     * @return
     */
    public static String nullSeVazio(String valor) {
        return verifica(valor).orElse(null);
    }

    /**
     * Retorna o proprio valor ou String vazia se for nulo
     *
     * @param valor
     * @return
     */
    public static String vazioSeNull(String valor) {
        return verifica(valor).orElse("");
    }

    /**
     * Retorna o proprio valor ou zero se for nulo
     *
     * @param valor
     * @return
     */
    public static Integer valorOuZeroSeNull(Integer valor) {
        return verifica(valor).orElse(0);
    }

    /**
     * Formata datas
     *
     * @param data
     * @param padrao
     * @return
     */
    public static String dateToString(Date data, String padrao) {

        if (!verifica(data).isPresent()) {
            return null;
        }
        return new SimpleDateFormat(padrao).format(data);
    }

    /**
     * Retorna true se a string informada for um numero inteiro.
     *
     * @param val
     * @return
     */
    public static boolean ehInteiro(String val) {
        return verifica(val).isPresent() && val.matches("^[0-9]*$");
    }

    /**
     * Completa com caracteres a esquerda.
     *
     * @param value String que sera completada
     * @param c     o caracter que sera inserido a esquerda da String
     * @param size  a quantidade de repetições do caracter
     * @return
     */
    public static String completarAEsquerda(String value, char c, int size) {
        String result = value;
        while (result.length() <= size) {
            result = c + result;
        }
        return result;
    }

    /**
     * Completa com zeros a esquerda ate o tamanho passado.
     *
     * @param value
     * @param length
     * @return
     */
    public static String completarComZerosAEsquerda(String value, int length) {
        int tam = value.length();
        StringBuilder result = new StringBuilder(value);

        for (int i = tam; i < length; i++) {
            result.insert(0, "0");
        }
        return result.substring(0,length);

    }

    /**
     * Completa com zeros a direita até o tamanho passado
     *
     * @param num
     * @param tam
     * @return
     */
    public static String completarZerosADireita(int num, int tam) {
        return String.format("%-" + tam + "s", num).replace(' ', '0');
    }

    /**
     * Remove zeros a esquerda.
     *
     * @param valor
     * @return
     */
    public static String removerZerosAEsquerda(String valor) {
        try {
            long parse = Long.parseLong(valor);
            return Long.toString(parse);

        } catch (NumberFormatException e) {
            return "0";
        }
    }


    /**
     * Formata o cpf/cnpj informado
     *
     * @return
     */
    public static String formatarCPFCNPJ(String dado) {
        try {

            Pattern pattern;
            Matcher matcher;

            if (dado.length() < 12) {
                pattern = Pattern.compile("(\\d{3})(\\d{3})(\\d{3})(\\d{2})");
                matcher = pattern.matcher(dado);
                if (matcher.matches()) {
                    dado = matcher.replaceAll("$1.$2.$3-$4");
                }
            } else {
                pattern = Pattern.compile("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})");
                matcher = pattern.matcher(dado);
                if (matcher.matches()) {
                    dado = matcher.replaceAll("$1.$2.$3/$4-$5");
                }
            }

            return dado;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formata telefone no padrão (xx)xxxx-xxxx ou (xx)xxxxx-xxxx
     * obs: o numero deve ser passado sem parenteses ou traço apenas numeros.
     *
     * @param tel
     * @return
     */
    public static String formatarTelefone(String tel) {
        //se o telefone tiver menos de 10 digitos nao faz a formatação
        if (tel.length() < 10 || !verifica(tel).isPresent()) return tel;

        String dig = tel.substring(0, 2);
        String num = tel.substring(2);
        String numFormat = "";

        if (num.length() > 8) {
            numFormat = num.substring(0, 5) + "-" + num.substring(5, 9);
        } else {
            numFormat = num.substring(0, 4) + "-" + num.substring(4, 8);
        }
        numFormat = "(" + dig + ") " + numFormat;
        return numFormat;
    }

    /**
     * Formata o cep passado.
     *
     * @param dado
     * @return
     */
    public static String formatarCep(String dado) {
        try {
            long cep = Long.parseLong(dado);
            long cep1, cep2;
            cep1 = cep / 1000;
            cep2 = cep % 1000;
            return String.format("%05d-%03d", cep1, cep2);
        } catch (Exception e) {
            return dado;
        }
    }

    /**
     * Completa os bytes da String com espaco em branco até o tamanho passado.
     *
     * @param dado
     * @param tamanho
     * @return
     */
    public static String completarBytesString(String dado, int tamanho, boolean reduzirTamanho) {

        int stringLength = dado.length();

        if (stringLength > tamanho) {
            if (reduzirTamanho) {
                return dado.substring(0, tamanho);
            } else {
                return dado;
            }
        }

        StringBuilder sb = new StringBuilder(dado);

        for (int i = stringLength; i < tamanho; i++) {
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * Completa os bytes da String com espaço em branco a esquerda até o tamanho passado.
     *
     * @param dado
     * @param tamanho
     * @return
     */
    public static String completarBytesAEsquerda(String dado, int tamanho) {
        int stringLength = dado.length();
        if (stringLength > tamanho) {
            return dado;
        }

        StringBuilder bytes = new StringBuilder();

        for (int i = stringLength; i < tamanho; i++) {
            bytes.append(" ");
        }

        return bytes.append(dado).toString();
    }

    /**
     * Retorna o nome abreviado
     *
     * @param nome
     * @return
     */
    public static String abreviarNome(String nome) {
        List<String> conjuncao = Arrays.asList("de", "da", "do", "das", "dos");
        StringTokenizer st = new StringTokenizer(nome, " ");
        StringBuilder nomeAbreviado = new StringBuilder();
        int i = 1;

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (i == 1) {
                nomeAbreviado.append(token.toUpperCase());
            } else if (conjuncao.indexOf(token.toLowerCase()) == -1) {
                nomeAbreviado.append(" ");
                nomeAbreviado.append(token.substring(0, 1).toUpperCase());

            }

            i++;
        }

        return nomeAbreviado.toString();
    }

    /**
     * Retorna o primeiro nome
     * Ex: Fulado de Tal - retorno = 'Fulano'
     *
     * @param nome
     * @return
     */
    public static String primeiroNome(String nome) {
        StringTokenizer st = new StringTokenizer(nome, " ");
        return st.nextToken();
    }

    /**
     * Limita o tamanho da String ate o limite passado.
     *
     * @param valor
     * @param limite
     * @return
     */
    public static String limitarTamanhoString(String valor, int limite) {
        if (valor.length() <= limite) return valor;
        return valor.substring(0, limite);
    }

    /**
     * Converte a primeira letra de cada palavra em maiusculo deixando
     * o resto em minusculo.
     *
     * @param nome
     * @return
     */
    public static String primeiraLetraMaiusculo(String nome) {
        StringTokenizer st = new StringTokenizer(nome.toLowerCase(), " ");
        StringBuilder primeiraMaiusculo = new StringBuilder();

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            primeiraMaiusculo.append(StringUtils.capitalize(token));
            if (st.hasMoreTokens()) {
                primeiraMaiusculo.append(" ");
            }
        }

        return primeiraMaiusculo.toString();
    }

    /**
     * Remove os espaços em branco da String, incluindo os espaços entre as palavras
     * Exemplo: (-) representa os espaços em branco
     * String: "-----Fulano--------de------Tal-----";
     * Retorno: "Fulano de Tal";
     *
     * @param s
     * @return
     */
    public static String removerEspacoEmBranco(String s) {
        StringTokenizer st = new StringTokenizer(s.trim(), " ");
        StringBuilder sb = new StringBuilder();
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            if (st.hasMoreTokens()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Remove tudo que não numeros de 0 a 9 da String passada.
     *
     * @param s
     * @return
     */
    public static String manterApenasNumeros(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    /**
     * Remove tudo que não for letras de (A a Z) inclusive acentuação
     *
     * @param s
     * @return
     */
    public static String manterApenasLetras(String s) {
        return s.replaceAll("[^a-zA-Z]", "");
    }

    /**
     * Remove Acentos da String
     *
     * @param str
     * @return
     */
    public static String removeAcentos(String str) {
        CharSequence cs = new StringBuilder(str == null ? "" : str);
        return Normalizer.normalize(cs, Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Formata a descricao do enum que vai para a view
     * Ex enum: 1-OPERACAO_PRESENCIAL saida: 1-Operacao Presencial
     *
     * @param valEnum
     * @return
     */
    public static String formatarEnum(String valEnum) {
        String replaceUnder = valEnum.replace("_", " ");
        int indexTraco = replaceUnder.indexOf("-");
        String codigo = replaceUnder.substring(0, indexTraco);
        String descricao = replaceUnder.substring(indexTraco + 1);

        descricao = primeiraLetraMaiusculo(descricao);
        return codigo.concat("-").concat(descricao);

    }

    /**
     * Retorna o ip da maquina
     *
     * @return
     * @throws UnknownHostException
     */
    public static String getIpLocalhost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName(); // retorna o name do computador na rede
        //return InetAddress.getLocalHost().getHostAddress(); // retorna o ip do computador na rede
    }

    /**
     * Retorna o valor informado criptografado segundo o algoritimo {@link MessageDigest} informado.
     *
     * @param valor
     * @param algoritmo
     * @return
     */
    private static String getValorCriptografado(final String valor, final String algoritmo) {
        try {
            if (!verifica(algoritmo).isPresent()) {
                throw new IllegalArgumentException("O algoritimo não foi especificado.");
            }

            if (!verifica(valor).isPresent()) {
                return "";
            }

            MessageDigest messageDigest = MessageDigest.getInstance(algoritmo);

            byte[] digest = messageDigest.digest(valor.getBytes(CHARSET_UFT_8));
            BigInteger hash = new BigInteger(1, digest);

            String crypto = hash.toString(16);
            if (crypto.length() % 2 != 0) {
                crypto = "0" + crypto;
            }
            return crypto;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Falha ao Criptografar o valor: " + valor + " em " + algoritmo + ".", e);
        }
    }

    /**
     * Retorna o valor Criptografado baseado no algoritimo de hash de 128bits unidirecional MD5.
     *
     * @param valor
     * @return
     * @throws IllegalArgumentException
     * @throws RuntimeException
     */
    public static String getValorCriptografadoMD5(final String valor) {
        return getValorCriptografado(valor, "MD5");
    }

    /**
     * Retorna o valor Criptografado baseado no algoritimo de hash SHA-1.
     *
     * @param valor
     * @return
     * @throws IllegalArgumentException
     * @throws RuntimeException
     */
    public static String getValorCriptografadoSHA1(final String valor) {
        return getValorCriptografado(valor, "SHA1");
    }
}