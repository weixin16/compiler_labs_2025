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
    public enum Stage {
        LEXER, PARSER, SEMANTIC, IR, MIPS
    }

    public static final Stage STAGE= Stage.LEXER;
//    public static final Stage STAGE= Stage.PARSER;
//    public static final Stage STAGE= Stage.SEMANTIC;
//    public static final Stage STAGE= Stage.IR;
//    public static final Stage STAGE= Stage.MIPS;

    public static void main(String[] args) {
        try {
            ErrorList.clear();
            String sourceCode = FileHandler.readTestFile();
            Lexer lexer = new Lexer();
            lexer.analyze(sourceCode);

            if(STAGE == Stage.LEXER){
                if(ErrorList.getErrors().isEmpty()){
                    FileHandler.writeLexerFile(lexer.getTokens());
                } else {
                    FileHandler.writeErrorFile(ErrorList.getErrors());
                }
                return;
            }

            Parser parser = new Parser(lexer.getTokens());
            CompUnit ast = parser.analyze();

            if(STAGE == Stage.PARSER){
                if(ErrorList.getErrors().isEmpty()){
                    FileHandler.writeParserFile(parser.getOutputs());
                } else {
                    FileHandler.writeErrorFile(ErrorList.getErrors());
                }
                return;
            }

            Visitor visitor = new Visitor(ast);
            visitor.analyze();

            if(STAGE == Stage.SEMANTIC){
                if(ErrorList.getErrors().isEmpty()){
                    FileHandler.writeSymbolFile(visitor.getAllSymbols());
                } else {
                    FileHandler.writeErrorFile(ErrorList.getErrors());
                }
                return;
            }

            if(ErrorList.getErrors().isEmpty()){
                SymbolManager symbolManager = visitor.getSymbolManager();
                IRBuilder irBuilder = new IRBuilder();
                IRGenerator irGenerator = new IRGenerator(ast,symbolManager,irBuilder);
                irGenerator.generate();
                List<Quad> quads = irBuilder.getIr();

                if(STAGE == Stage.IR){
                    FileHandler.writeErrorFile(ErrorList.getErrors());
                    return;
                }

                MipsGenerator mipsGenerator = new MipsGenerator(quads);
                List<String> mipsCode = mipsGenerator.generate();

                if(STAGE == Stage.MIPS){
                    FileHandler.writeMipsFile(mipsCode);
                }
            } else {
                FileHandler.writeErrorFile(ErrorList.getErrors());
            }


        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
