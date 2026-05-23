package br.com.swconsultoria.apidfe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

class ApiNFeApplicationTests {

    public static void main(String[] args) throws IOException {
        System.out.println(fileToByte("/d/teste/certificado.pfx"));
    }

    private static String fileToByte(String caminhoArquivo) throws IOException {
        byte[] fileContent = Files.readAllBytes(new File(caminhoArquivo).toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

}
