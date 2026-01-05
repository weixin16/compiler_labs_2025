package frontend.error;

public class Error {
    String errorType;
    Integer lineNum;

    public Error(String errorType, Integer lineNum) {
        this.errorType = errorType;
        this.lineNum = lineNum;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public Integer getLineNum() {
        return lineNum;
    }

    public void setLineNum(Integer lineNum) {
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        return lineNum + " " + errorType;
    }

}
