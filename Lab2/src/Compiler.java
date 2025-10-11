import error.CompileError;
import error.ErrorRecorder;
import exception.LexerException;
import frontend.Lexer;
import frontend.Token;
import frontend.TokenList;

import java.io.*;

public class Compiler {
    private static ErrorRecorder errorRecorder = new ErrorRecorder();
    private static TokenList tokenList = new TokenList();

    public static void main(String[] args) throws IOException{
        //String filePath = Compiler.class.getResource("/testfile.txt").getPath();
        String filePath = "testfile.txt";
        //输入的代码文件
        FileInputStream fileInputStream = new FileInputStream(filePath);
        Lexer lexer = new Lexer(new InputStreamReader(fileInputStream), errorRecorder);

        printLexerResult(lexer);
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

}
