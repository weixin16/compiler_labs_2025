import frontend.FileHandler;
import frontend.Lexer;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) {
        try {

            String sourceCode = FileHandler.readTestFile();
            Lexer lexer = new Lexer();
            lexer.analyze(sourceCode);
            if(lexer.hasError()){
                FileHandler.writeErrorFile(lexer.getErrors());
            } else {
                FileHandler.writeLexerFile(lexer.getTokens());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
