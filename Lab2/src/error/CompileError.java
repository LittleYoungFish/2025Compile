package error;

/**
 * 错误类
 */
public class CompileError {
    private final ErrorType type;
    private final int lineNum;

    public CompileError(ErrorType type, int lineNum) {
        this.type = type;
        this.lineNum = lineNum;
    }
    public ErrorType getType() {
        return type;
    }
    public int getLineNum() {
        return lineNum;
    }
}
