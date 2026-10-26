package portal_voluntario.api;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> uploadSize() {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "Envie um arquivo de até 5 MB"));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation() {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira os campos informados"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> authentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas ou conta indisponível"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handle(ResponseStatusException exception) {
        String detail = exception.getReason() == null ? "Não foi possível concluir a operação" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(ProblemDetail.forStatusAndDetail(exception.getStatusCode(), detail));
    }
}
