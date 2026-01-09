package genai.genaiprojectbackend.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;


public class FileNameValidator implements ConstraintValidator<FileNameValid, String>, Annotation {

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        //String pattern = ""; // Example pattern: only allows alphanumeric characters, dots, underscores, and hyphens
        //RegexValidator regexValidator = new RegexValidator(pattern);
        //TODO add more fileName validation like only allowing laz for example when we decide on that
        return true; //regexValidator.isValid(s);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return FileNameValid.class;
    }
}


