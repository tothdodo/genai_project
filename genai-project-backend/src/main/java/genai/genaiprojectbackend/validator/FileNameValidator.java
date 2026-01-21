package genai.genaiprojectbackend.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;


public class FileNameValidator implements ConstraintValidator<FileNameValid, String>, Annotation {

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return true;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return FileNameValid.class;
    }
}


