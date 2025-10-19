package frontend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileHandler {

    public static String readTestFile() throws IOException {
        return Files.readString(Paths.get("testfile.txt"), StandardCharsets.UTF_8);

    }

    public static void writeLexerFile(List<Token> tokens) throws IOException {
        new File("lexer.txt").delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("lexer.txt"))) {
            for (Token token : tokens) {
                bw.write(token.getTokenType().toString() + " " + token.getLexeme());
                bw.newLine();
            }
        }
    }

    public static void writeErrorFile(List<String> errors) throws IOException {
        new File("error.txt").delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("error.txt"))) {
            for (String error : errors) {
                bw.write(error);
                bw.newLine();
            }
        }
    }


}
