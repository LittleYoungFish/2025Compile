package frontend.lexer;

public enum TokenType {
    IDENFR("ident"), //标识符
    INTCON("int const"), //数字常量
    STRCON("format string"), //字符串常量
    CONSTTK("const"), //const
    INTTK("int"), //int
    STATICTK("static"), //static
    BREAKTK("break"), //break
    CONTINUETK("continue"), //continue
    IFTK("if"), //if
    MAINTK("main"), //main
    ELSETK("else"), //else
    NOT("!"), // !
    AND("&&"), // &&
    OR("||"), // ||
    FORTK("for"), // for
    RETURNTK("return"), //return
    VOIDTK("void"), //void
    PLUS("+"), //+
    MINU("-"), //-
    PRINTFTK("printf"), // printf
    MULT("*"), // *
    DIV("/"), // /
    MOD("%"), // %
    LSS("<"), // <
    LEQ("<="), // <=
    GRE(">"), // >
    GEQ(">="), // >=
    EQL("=="), // ==
    NEQ("!="), // !=
    SEMICN(";"), // ;
    COMMA(","), // ,
    LPARENT("("), // (
    RPARENT(")"), // )
    LBRACK("["), // [
    RBRACK("]"), // ]
    LBRACE("{"), // {
    RBRACE("}"), // }
    ASSIGN("="), // =

    ERROR("error");

    private final String value;

    TokenType(String s){
        value = s;
    }

    @Override
    public String toString(){
        return value;
    }

    // 获取标识符的类型
    public static TokenType getTokenType(String input) {
        // 先处理多字符情况（优先级更高，避免被单字符匹配覆盖）
        switch (input) {
            // 多字符符号
            case "<=" -> { return LEQ; }
            case ">=" -> { return GEQ; }
            case "==" -> { return EQL; }
            case "!=" -> { return NEQ; }
            case "&&" -> { return AND; }
            case "||" -> { return OR; }
            // 关键字
            case "const" -> { return CONSTTK; }
            case "int" -> { return INTTK; }
            case "static" -> { return STATICTK; }
            case "break" -> { return BREAKTK; }
            case "continue" -> { return CONTINUETK; }
            case "if" -> { return IFTK; }
            case "main" -> { return MAINTK; }
            case "else" -> { return ELSETK; }
            case "for" -> { return FORTK; }
            case "return" -> { return RETURNTK; }
            case "void" -> { return VOIDTK; }
            case "printf" -> { return PRINTFTK; }
        }

        // 再处理单字符情况（输入长度为1时）
        if (input.length() == 1) {
            char c = input.charAt(0);
            switch (c) {
                case '!' -> { return NOT; }
                case '+' -> { return PLUS; }
                case '-' -> { return MINU; }
                case '*' -> { return MULT; }
                case '/' -> { return DIV; }
                case '%' -> { return MOD; }
                case '<' -> { return LSS; }
                case '>' -> { return GRE; }
                case ';' -> { return SEMICN; }
                case ',' -> { return COMMA; }
                case '(' -> { return LPARENT; }
                case ')' -> { return RPARENT; }
                case '[' -> { return LBRACK; }
                case ']' -> { return RBRACK; }
                case '{' -> { return LBRACE; }
                case '}' -> { return RBRACE; }
                case '=' -> { return ASSIGN; }
            }
        }

        // 若都不匹配，返回标识符（IDENFR）或错误（ERROR）
        // （根据实际逻辑调整，例如标识符通常是字母/下划线开头的字符串）
        return isIdentifier(input) ? IDENFR : ERROR;
    }

    // 辅助方法：判断是否为标识符（简化版，实际可能需要更复杂的校验）
    private static boolean isIdentifier(String input) {
        if (input.isEmpty()) return false;
        char first = input.charAt(0);
        return (first >= 'a' && first <= 'z')
                || (first >= 'A' && first <= 'Z')
                || first == '_';
    }
}
