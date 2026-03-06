package id.ac.ui.cs.advprog.yomu.reading.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "texts")
public class Text {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String createdByUserId;

    private boolean published = true;

    private Instant createdAt = Instant.now();

    public Text(){}

    public Text(String title, String content, Category category, String createdByUserId){
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdByUserId = createdByUserId;
    }

    public Long getId(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getContent(){
        return content;
    }

    public Category getCategory(){
        return category;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void setCategory(Category category){
        this.category = category;
    }
}