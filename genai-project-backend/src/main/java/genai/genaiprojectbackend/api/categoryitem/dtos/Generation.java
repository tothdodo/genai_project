package genai.genaiprojectbackend.api.categoryitem.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
public class Generation {;
    private final String summary;
    private List<FlashcardDTO> flashcards;
}
