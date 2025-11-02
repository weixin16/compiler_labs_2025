import frontend.ast.CompUnit;
import frontend.error.ErrorList;
import frontend.FileHandler;
import frontend.Lexer;
import frontend.Parser;

import java.io.IOException;

public class Compiler {
    public static ErrorList errors;
    public static void main(String[] args) {
        try {
            ErrorList.clear();
            String sourceCode = FileHandler.readTestFile();
            Lexer lexer = new Lexer();
            lexer.analyze(sourceCode);

            Parser parser = new Parser(lexer.getTokens());
            parser.parse();

            if (!lexer.hasError() && !parser.hasError()) {
                FileHandler.writeLexerFile(lexer.getTokens());
                FileHandler.writeParserFile(parser.getOutputs());
            } else {
                FileHandler.writeErrorFile(ErrorList.getErrors());
            }


        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
