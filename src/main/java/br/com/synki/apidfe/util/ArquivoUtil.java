package br.com.synki.apidfe.util;

import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 */
public class ArquivoUtil {

    /**
     * Transforma o byte[] em xml
     *
     * @param gZip
     * @return
     * @throws IOException
     * @throws JAXBException
     */
    public static String descompactaXml(byte[] gZip) throws IOException {
        if (!Util.verifica(gZip).isPresent()) {
            return "";
        }
        try (final GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(gZip));
             final StringWriter stringWriter = new StringWriter()) {
            IOUtils.copy(gzipInput, stringWriter);
            return stringWriter.toString();
        }
    }

    /**
     * Transforma o Xml em bytes Compactado
     *
     * @param sXml
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static byte[] compactaXml(String sXml) throws IOException {
        if (!Util.verifica(sXml).isPresent()) {
            return null;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        try (OutputStream outStream = new GZIPOutputStream(obj)) {
            outStream.write(sXml.getBytes(StandardCharsets.UTF_8));
        }
        return obj.toByteArray();
    }

    /**
     * Retorna a String do XML que estara na URL
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String leConteudoUrl(String url) throws IOException {
        InputStream in = new URL(url).openStream();
        try {
            return IOUtils.toString(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }


}
