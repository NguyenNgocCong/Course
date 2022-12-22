package com.courses.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotEmpty;

@Entity @Setter
@Getter
@AllArgsConstructor @NoArgsConstructor @ToString
public class Package extends BaseDomain{
    private String title;
    private String excerpt;
    private int duration;
    private String image;
    private long views;
    
    @NotEmpty(message = "Write something for the love of Internet...")
    @Lob
    private String description;
    
    private boolean status;
    @Column(name = "list_price")
    private double listPrice;
    @Column(name = "sale_price")
    private double salePrice;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    @JsonIgnore
    private Subject subject;

    public Package(String title, String excerpt, int duration, String image, String description, boolean status, double listPrice, double sale_price) {
        this.title = title;
        this.excerpt = excerpt;
        this.duration = duration;
        this.description = description;
        this.status = status;
        this.listPrice = listPrice;
        this.salePrice = sale_price;
        this.setImage(image);
    }

    public void increaseView() {
        this.views += 1;
    }
}
