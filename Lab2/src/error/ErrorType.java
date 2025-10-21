package error;

public enum ErrorType {
    ILLEGAL_SYMBOL("a"),
    NAME_REDEFINE("b"),
    UNDEFINED_NAME("c"),
    NUM_OF_PARAM_NOT_MATCH("d"),
    TYPE_OF_PARAM_NOT_MATCH("e"),
    RETURN_NOT_MATCH("f"),
    RETURN_MISS("g"),
    CHANGE_CONST_VAL("h"),
    SEMICN_MISS("i"),
    RPARENT_MISS("j"),
    RBRACK_MISS("k"),
    PRINTF_NOT_MATCH("l"),
    BREAK_OR_CONTINUE_ERROR("m");;


    private final String value;

    ErrorType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
