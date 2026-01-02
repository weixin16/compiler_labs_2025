import backend.mips.MipsGenerator;
import frontend.FileHandler;
import frontend.Visitor;
import frontend.ast.CompUnit;
import frontend.error.ErrorList;
import frontend.Lexer;
import frontend.Parser;
import frontend.symbol.SymbolManager;
import midend.ir.IRBuilder;
import midend.ir.IRGenerator;
import midend.ir.Quad;

import java.io.IOException;
import java.util.List;

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
                SymbolManager symbolManager = visitor.getSymbolManager();

                IRBuilder irBuilder = new IRBuilder();
                IRGenerator irGenerator = new IRGenerator(ast,symbolManager,irBuilder);
                irGenerator.generate();
                List<Quad> quads = irBuilder.getIr();
                FileHandler.writeIRFile(quads);

                MipsGenerator mipsGenerator = new MipsGenerator(quads);
                FileHandler.writeMipsFile(mipsGenerator.generate());

            } else {
                FileHandler.writeErrorFile(ErrorList.getErrors());
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
