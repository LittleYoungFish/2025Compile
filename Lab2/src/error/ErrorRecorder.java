package error;

import java.util.ArrayList;
import java.util.List;

/**
 * 错误记录
 */
public class ErrorRecorder {
    private final List<CompileError> errorList = new ArrayList<CompileError>();

    public void addError(CompileError error) {
        errorList.add(error);
    }

    public void addError(ErrorType errorType, int lineNum) {
        addError(new CompileError(errorType, lineNum));
    }

    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public List<CompileError> getErrors() {
        return errorList;
    }
}
