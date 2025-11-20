package frontend;

import frontend.symbol.Symbol;
import frontend.token.Token;
import frontend.token.TokenType;

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
                if(token.getTokenType()== TokenType.EOF) continue;
                bw.write(token.getTokenType().toString() + " " + token.getLexeme());
                bw.newLine();
            }
        }

    }

    public static void writeErrorFile(List<String> errors) throws IOException {
        new File("error.txt").delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("error.txt"))) {
            for(String error : errors){
                bw.write(error);
                bw.newLine();
            }
        }
    }

    public static void writeParserFile(List<String> strings) throws IOException {
        new File("parser.txt").delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("parser.txt"))) {
            for (String string : strings) {
                bw.write(string);
                bw.newLine();
            }
        }
    }

    public static void writeSymbolFile(List<Symbol> symbols) throws  IOException {
        new File("symbol.txt").delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("symbol.txt"))) {
            for (Symbol symbol : symbols){
                if(symbol.getName().equals("getint")) continue;
                bw.write(symbol.getScopeId() + " " + symbol.getName() + " " + symbol.getType().name());
                bw.newLine();
            }
        }
    }


}
