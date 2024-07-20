package com.stellarnear;

import java.util.List;
import java.util.Objects;

public class Card {
    private String rarity;
    private String mana_cost;
    private int cmc;
    private String type_line;
    private List<String> color_identity;
    private String name;
    private String commanderLegality;
    private String oracleText;
    private Double priceUsd;
    private String typeBoard;
    private String deckName;



    // Constructor
    public Card(String name,String rarity, String mana_cost, int cmc, String type_line, List<String> color_identity, String commanderLegality, String oracle_text, Double price_usd, String typeBoard, String deckName) {
        this.name=name;
        this.rarity = rarity;
        this.mana_cost = mana_cost;
        this.cmc = cmc;
        this.type_line = type_line;
        this.color_identity = color_identity;
        this.commanderLegality=commanderLegality;
        this.oracleText = oracle_text;
        this.priceUsd=price_usd;
        this.typeBoard= typeBoard;
        this.deckName=deckName;
    }

    public String getName() {
        return name;
    }

    // Getters and setters
    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getMana_cost() {
        return mana_cost;
    }

    public void setMana_cost(String mana_cost) {
        this.mana_cost = mana_cost;
    }

    public int getCmc() {
        return cmc;
    }

    public void setCmc(int cmc) {
        this.cmc = cmc;
    }

    public String getType_line() {
        return type_line;
    }

    public void setType_line(String type_line) {
        this.type_line = type_line;
    }

    public List<String> getColor_identity() {
        return color_identity;
    }

    public void setColor_identity(List<String> color_identity) {
        this.color_identity = color_identity;
    }

    public String getCommanderLegality() {
        return commanderLegality;
    }

    public String getOracleText() {
        return oracleText;
    }


    public String getTypeBoard() {
        return typeBoard;
    }

    public String getDeckName() {
        return deckName;
    }

    public Double getPriceUsd() {
        return priceUsd;
    }

       @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(name, card.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
