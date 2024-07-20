package com.stellarnear;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hello world!
 */
public final class MoxfieldProxyBuyComparer {

    private static CustomLog log = new CustomLog(MoxfieldProxyBuyComparer.class);

    private MoxfieldProxyBuyComparer() {
    }


    private static String user = "StellarNear";
    private static String notBuyedDeck = "TBDzmy5Wj0K21AevzAd5Bw";
    private static boolean allowConsiderBoard=false;

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        long startTotal = System.currentTimeMillis();

  

        List<UserDataDeck> allDecksForUser = getAllDeckForUser(user);

        List<Card> allCollectedCard = new ArrayList<>();
        UserDataDeck treatDeck = null;
        log.info("Found " + allDecksForUser.size() + " decks");
        for (UserDataDeck deck : allDecksForUser) {
            if (deck.getPublicId().equalsIgnoreCase(notBuyedDeck)) {
                treatDeck = deck;
                continue;
            }
            log.info("Treating deck " + deck.getName());
            List<Card> cardsFromDeck = getDeckListFor(deck);
            log.info("Found " + cardsFromDeck.size() + " cards");
            allCollectedCard.addAll(cardsFromDeck);
        }


        log.info("Now parsing the cards to buy and to proxy for deck : " + treatDeck.getName());
        List<Card> cardsFromTargetDeck = getDeckListFor(treatDeck);

        int nProx = 0;
        int nMaybe = 0;
        int nBuy = 0;
        Double totalUsdProx = 0.0;
        Double totalUsdMaybe = 0.0;
        Double totalUsdBuy = 0.0;
        try (PrintWriter outBuy = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream("OUT/newToBuyCards.dat"), StandardCharsets.UTF_8))) {

            try (PrintWriter outProx = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream("OUT/alreadyHaveCards.dat"), StandardCharsets.UTF_8))) {
                        outProx.println("Cardname;FoundInDeck;BoardType");
                try (PrintWriter sidedCard = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream("OUT/sidedCards.dat"), StandardCharsets.UTF_8))) {
                            sidedCard.println("Cardname;FoundInDeck;BoardType");
                    for (Card card : cardsFromTargetDeck) {

                        Card matchingCardMain = findMatchingCard(card,allCollectedCard,"mainboard");
                        Card matchingCardSide = findMatchingCard(card,allCollectedCard,"sideboard");
                        if(matchingCardSide==null && allowConsiderBoard){
                            matchingCardSide = findMatchingCard(card,allCollectedCard,"maybeboard");
                        }
                        if (matchingCardMain!=null || matchingCardSide!=null) {
                            if(matchingCardMain!=null){
                                outProx.println(matchingCardMain.getName()+";"+matchingCardMain.getDeckName()+";"+matchingCardMain.getTypeBoard());
                                totalUsdProx += card.getPriceUsd();
                                nProx++;
                            } else {
                                sidedCard.println(matchingCardSide.getName()+";"+matchingCardSide.getDeckName()+";"+matchingCardSide.getTypeBoard());
                                totalUsdMaybe += card.getPriceUsd();
                                nMaybe++;
                            }  
                        } else {
                            outBuy.println(card.getName());
                            totalUsdBuy += card.getPriceUsd();
                            nBuy++;
                        }
                    }
                }
            }
        }
        Double totalCollectUsd = 0.0;
        Set<Card> singleCardByNameForPrice= new HashSet<>();
        singleCardByNameForPrice.addAll(allCollectedCard);
        for(Card card: singleCardByNameForPrice){
            totalCollectUsd += card.getPriceUsd();
        }
    
        long endTotal = System.currentTimeMillis();
        log.info("MoxfieldProxyBuyComparer ended it took a total time of " + convertTime(endTotal - startTotal));
        log.info("The total collection of " + user + " has " + allCollectedCard.size() + " cards (estimated at "
                + String.format("%.2f", totalCollectUsd) + " usd)");
        log.info("The deck " + treatDeck.getName() + " contains " + nBuy + " new cards to buy (estimated at "
                + String.format("%.2f", totalUsdBuy) + " usd) and " + nMaybe + " to maybe if there in side or maybeboard  (economy of "
                + String.format("%.2f", totalUsdMaybe) + " usd)) and " + nProx + " to proxy (economy of "
                + String.format("%.2f", totalUsdProx) + " usd)).");
    }

    private static Card findMatchingCard(Card targetCard, List<Card> allCollectedCard, String typeboard) {
       for(Card card: allCollectedCard){
        if(card.equals(targetCard) && card.getTypeBoard().equalsIgnoreCase(typeboard)){
            return card;
        }
       }
       return null;
    }

    private static void setConenction(HttpURLConnection connection) {
        connection.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        // connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
        // connection.setRequestProperty("Accept-Language",
        // "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        // HERE go to the url and get the refresh from the header with dev mod on chrome
        connection.setRequestProperty("Cookie", "refresh_token=13c67b3b-3cf8-465a-aacd-3bb73f8c04f1");
    }

    private static List<UserDataDeck> getAllDeckForUser(String user) throws MalformedURLException {

        // ex https://api2.moxfield.com/v2/users/Nonoein/decks
        String userUrl = "https://api2.moxfield.com/v2/users/" + user + "/decks";
        URL url = new URL(userUrl);
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            setConenction(connection);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                String jsonResponse = responseBuilder.toString();

                // Parse JSON
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode dataNode = rootNode.path("data");
                List<UserDataDeck> allUserData = new ArrayList<>();

                if (dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        UserDataDeck child = new UserDataDeck();
                        child.setOwner(user);
                        child.setPublicId(node.path("publicId").asText());
                        child.setName(node.path("name").asText());
                        allUserData.add(child);
                    }
                }
                return allUserData;
            } catch (Exception e1) {
                log.err("Error reading the user data", e1);
            }
        } catch (Exception e) {
            log.err("Error getting the connection to user data", e);
        }
        return new ArrayList<>();
    }

    private static List<Card> getDeckListFor(UserDataDeck deck) throws MalformedURLException {
        // ex https://api.moxfield.com/v2/decks/all/HxV33izihky7KTwjU0ER9w
        String deckUrl = "https://api.moxfield.com/v2/decks/all/" + deck.getPublicId();
        URL url = new URL(deckUrl);
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) url.openConnection();
            setConenction(connection);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                String jsonResponse = responseBuilder.toString();

                // Parse the JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(jsonResponse);

                String deckName = rootNode.path("name").asText();

                // Assuming "mainboard" is a direct child of the root node and contains the
                // cards
        
                List<String> typeBoards = Arrays.asList("mainboard", "sideboard");
                if(allowConsiderBoard){
                    typeBoards.add("maybeboard");
                }
                List<Card> deckCards = new ArrayList<>();
                for (String typeBoard : typeBoards) {
                    JsonNode board = rootNode.path(typeBoard);

                    Iterator<Map.Entry<String, JsonNode>> fields = board.fields();

                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        JsonNode cardNode = entry.getValue().path("card");

                        byte[] utf8Bytes = cardNode.path("name").asText().getBytes(StandardCharsets.UTF_8);
                        String nameNorm = Normalizer.normalize(new String(utf8Bytes, StandardCharsets.UTF_8),
                                Normalizer.Form.NFD);
                        ;
                        String name = nameNorm.replaceAll("\\p{M}", "");

                        String rarity = cardNode.path("rarity").asText();
                        String mana_cost = cardNode.path("mana_cost").asText();
                        int cmc = cardNode.path("cmc").asInt();
                        String type_line = cardNode.path("type_line").asText();

                        String oracle_text = cardNode.path("oracle_text").asText();
                        List<String> color_identity = objectMapper.convertValue(cardNode.path("color_identity"),
                                new TypeReference<List<String>>() {
                                });

                        String commanderLegality = cardNode.path("legalities").path("commander").asText();

                        Double price_usd = cardNode.path("prices").path("usd").asDouble();
                        Card card = new Card(name, rarity, mana_cost, cmc, type_line, color_identity, commanderLegality,
                                oracle_text, price_usd, typeBoard, deckName);
                        deckCards.add(card);
                    }
                }
                return deckCards;
            } catch (Exception e1) {
                e1.printStackTrace();
                log.err("Error reading the user data", e1);

            }
        } catch (Exception e) {
            log.err("Error getting the connection to user data", e);
        }
        return new ArrayList<>();
    }

    private static String convertTime(long l) {
        int nHour = (int) (((l / 1000) / 60) / 60);
        if (nHour > 0) {
            int nMinute = (int) ((l / 1000) / 60) - 60 * nHour;
            return nHour + " hours " + nMinute + " minutes";
        } else {
            int nMinute = (int) ((l / 1000) / 60);
            if (nMinute > 0) {
                return nMinute + " minutes";
            } else {
                return (int) (l / 1000) + " seconds";
            }
        }
    }

}
