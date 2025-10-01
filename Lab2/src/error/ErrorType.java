package error;

public enum ErrorType {
    ILLEGAL_SYMBOL("a");


    private final String value;

    ErrorType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
