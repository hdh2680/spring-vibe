package springVibe.system.exception;

/**
 * 애플리케이션 공통 예외 베이스.
 */
public class BaseException extends RuntimeException {

    private final String code;

    public BaseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
