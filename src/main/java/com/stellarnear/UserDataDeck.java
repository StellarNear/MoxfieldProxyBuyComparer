package com.stellarnear;

import java.util.List;

public class UserDataDeck {
    private String publicId;
    private String name;
    private List<Card> cardList;
    private String owner;

    // Getters and setters
    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addCardList(List<Card> cardListIn) {
        this.cardList = cardListIn;
    }

    
    public List<Card> getCardList() {
        return cardList;
    }

    public void setOwner(String user) {
        this.owner=user;
    }

    public String getOwner() {
        return owner;
    }
}
