package com.marsarmy.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class ProductStatistics implements Serializable {

    private Long upc;
    private String name;
    private String color;
    private String brand;
    private String category;
    private Integer price;
    private Long quantitySold;
}
