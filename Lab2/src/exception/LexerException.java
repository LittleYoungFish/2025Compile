package exception;

public class LexerException extends CompilerException{
    public LexerException(){
        super();
    }
    public LexerException(String message){
        super(message);
    }
    public LexerException(Throwable cause){
        super(cause);
    }
    public LexerException(String message, Throwable cause){
        super(message, cause);
    }
}
