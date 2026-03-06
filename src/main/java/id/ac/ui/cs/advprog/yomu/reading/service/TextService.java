package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.reading.model.*;
import id.ac.ui.cs.advprog.yomu.reading.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextService {

    private final TextRepository textRepository;
    private final CategoryRepository categoryRepository;

    public TextService(TextRepository textRepository,
                       CategoryRepository categoryRepository){
        this.textRepository = textRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Text> getAllTexts(){
        return textRepository.findByPublishedTrue();
    }

    public Text getTextById(Long id){
        return textRepository.findById(id).orElseThrow();
    }

    public Text createText(String title, String content, Long categoryId, String userId){

        Category category = categoryRepository.findById(categoryId).orElseThrow();

        Text text = new Text(title, content, category, userId);

        return textRepository.save(text);
    }

}