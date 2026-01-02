package backend.mips.data;

public class StringConst {
    private final String label;
    private final String content;

    public StringConst(String label, String content) {
        this.label = label;
        this.content = content;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": .asciiz \"");
        for(char c : content.toCharArray()){
            switch (c){
                case '\\' -> sb.append("\\\\");
                case '\"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

}
