import backend.ir.Module;
import error.CompileError;
import error.ErrorRecorder;
import exception.LexerException;
import exception.ParserException;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.lexer.TokenList;
import frontend.parser.Parser;
import frontend.parser.node.CompUnit;
import frontend.parser.node.Node;
import frontend.symtable.SymbolTable;
import frontend.visitor.Visitor;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Compiler {
    private static ErrorRecorder errorRecorder = new ErrorRecorder();
    private static TokenList tokenList = new TokenList();

    public static void main(String[] args) throws IOException, ParserException, LexerException {
        //String filePath = Compiler.class.getResource("/testfile.txt").getPath();
        String filePath = "testfile.txt";
        // 输入的代码文件
        FileInputStream fileInputStream = new FileInputStream(filePath);
        // 分词器
        Lexer lexer = new Lexer(new InputStreamReader(fileInputStream), errorRecorder);

        //printLexerResult(lexer);
        // 语法分析器
        Parser parser = new Parser(lexer, errorRecorder);

        // 语法分析
        //parser(parser);
        //parser.parse();

        // 语义分析
        //visit(parser);

        // 中间代码生成一
        generateLLVM(parser);

        printError();
    }

    public static void lexer(Lexer lexer) throws LexerException, IOException {
        while (lexer.next()){
            Token token = lexer.getToken();
            System.out.println("读取到Token: " + token.getTokenType().name() + " " + token.getTokenContent());
            tokenList.addToken(token);
        }
    }

    //输出分词结果到文件中
    public static void printLexerResult(Lexer lexer) throws IOException{
        // 将所有需要自动关闭的流都放在try的括号中
        try(
                FileOutputStream lexerOutputStream = new FileOutputStream("lexer.txt");
                PrintStream lexerPrintStream = new PrintStream(lexerOutputStream);
                FileOutputStream errorOutputStream = new FileOutputStream("error.txt");
                PrintStream errorPrintStream = new PrintStream(errorOutputStream);
        ){
            //进行分词
            lexer(lexer);

            //错误信息
            if(errorRecorder.hasErrors()){
                for(CompileError error : errorRecorder.getErrors()){
                    errorPrintStream.printf("%d %s\n", error.getLineNum(), error.getType());
                }
            }
            errorPrintStream.flush();

            //分词结果
            if(!errorRecorder.hasErrors()){
                for(Token token : tokenList.getTokenList()) {
                    lexerPrintStream.printf("%s %s\n", token.getTokenType().name(), token.getTokenContent());
                }
            }
            lexerPrintStream.flush();
        } catch (LexerException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parser(Parser parser) throws IOException, LexerException, LexerException, ParserException {
        try(
            FileOutputStream fileOutputStream = new FileOutputStream("parser.txt")){
            PrintStream out = new PrintStream(fileOutputStream);
            Node result = parser.parse();

            result.walk(
                    // Consumer 负责遇到节点时“做什么”
                    out::println, // 对终端符号的操作是 out::println（打印终端符号到文件）
                    nonTerminalSymbol -> { // 对非终端符号的操作是 “过滤掉 BlockItem、Decl、BType 后打印”
                        String type = nonTerminalSymbol.getType();
                        if(!type.equals("BlockItem")
                            && !type.equals("Decl")
                            && !type.equals("BType")
                        ){
                            out.println(nonTerminalSymbol);
                        }
                    }
            );
        }
    }

    public static void visit(Parser parser) throws FileNotFoundException {
        try(
                FileOutputStream fileOutputStream = new FileOutputStream("symbol.txt")
        ){
            PrintStream out = new PrintStream(fileOutputStream);
            Node result = parser.parse(); // 进行语义分析

            Visitor visitor = new Visitor(errorRecorder);
            SymbolTable symbolTable = visitor.visitCompUnit((CompUnit) result);

            // 全局作用域序号为1
            int[] initScopeId = {1};
            symbolTable.printSymbolTable(out, 0, initScopeId);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        } catch (LexerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateLLVM(Parser parser) throws IOException {
        try(
                FileOutputStream fileOutputStream = new FileOutputStream("llvm_ir.txt")
        ){
            PrintStream out = new PrintStream(fileOutputStream);
            Node result = parser.parse();

            Visitor visitor = new Visitor(errorRecorder);
            Module module = visitor.generateIR(result);

            out.print("""
                    declare i32 @getint()
                    declare void @putint(i32)
                    declare void @putch(i32)
                    declare void @putstr(i8*)
                    
                    """);
            module.dump(out);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        } catch (LexerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printError() throws IOException{
        try(
                FileOutputStream errorOutputStream = new FileOutputStream("error.txt");
                PrintStream errorPrintStream = new PrintStream(errorOutputStream);
        ) {
            //错误信息
            if(errorRecorder.hasErrors()){
                // 获取错误列表并按行号升序排序
                List<CompileError> errors = errorRecorder.getErrors();
                Collections.sort(errors, Comparator.comparingInt(CompileError::getLineNum));

                // 遍历输出排序后的错误
                for (CompileError error : errors) {
                    errorPrintStream.printf("%d %s\n", error.getLineNum(), error.getType());
                }
            }
            errorPrintStream.flush();
        }
    }
}
