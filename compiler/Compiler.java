import frontend.Visitor;
import frontend.ast.CompUnit;
import frontend.error.ErrorList;
import frontend.FileHandler;
import frontend.Lexer;
import frontend.Parser;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) {
        try {
            ErrorList.clear();
            String sourceCode = FileHandler.readTestFile();
            Lexer lexer = new Lexer();
            lexer.analyze(sourceCode);

            Parser parser = new Parser(lexer.getTokens());
            CompUnit ast = parser.analyze();

            Visitor visitor = new Visitor(ast);
            visitor.analyze();

            if (ErrorList.getErrors().isEmpty()) {
                FileHandler.writeLexerFile(lexer.getTokens());
                FileHandler.writeParserFile(parser.getOutputs());
                FileHandler.writeSymbolFile(visitor.getAllSymbols());
            } else {
                FileHandler.writeErrorFile(ErrorList.getErrors());
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
