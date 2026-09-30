package com.stellarnear;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.jasypt.util.text.BasicTextEncryptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hello world!
 */
public final class MoxfieldProxyBuyComparer {

    private static CustomLog log = new CustomLog(MoxfieldProxyBuyComparer.class);

    private static String encryptedAgent = "+lmuo3n0nvJINPsXppfLh5wbQk1fkJJ3BY6+5cA/WhfxENvuJYGQTQ==";
    private static String customPassword;
    private static String decryptedAgent;

    private MoxfieldProxyBuyComparer() {
    }

    private static String user = "StellarNear";

    // yuriko kJcEKNJ3P0eZRVVQHdHzeg
    // roxanne RD2kbSMKAUGgJxtM2cE9Pw
    // zethi 9AYzD7WrYki4yDLgIGhBAA

    private static List<String> notBuyedDecks = Arrays.asList("pO3UVMHJ4UWZiGrJmUhayA");

    private static boolean allowConsiderBoard = true;

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        long startTotal = System.currentTimeMillis();

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the custom password to decrypt the agent moxfield : ");
        customPassword = scanner.nextLine();

        if (customPassword.isEmpty()) {
            log.err("You must provide the custom password");
        }

        try {
            BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
            textEncryptor.setPassword(customPassword);
            decryptedAgent = textEncryptor.decrypt(encryptedAgent);

        } catch (Exception e) {
            log.err("Invalid custom password or encrypted value !");
            throw new Exception("Invalid custom password or encrypted value !");
        }

        List<UserDataDeck> allDecksForUser = getAllDeckForUser(user);
        log.info("Found " + allDecksForUser.size() + " decks");
        List<Card> allCollectedCard = new ArrayList<>();
        List<UserDataDeck> treatDecks = new ArrayList<>();

        for (UserDataDeck deck : allDecksForUser) {
            if (notBuyedDecks.contains(deck.getPublicId())) {
                treatDecks.add(deck);
                continue;
            }
            log.info("Treating deck " + deck.getName());
            getDeckListFor(deck);
            log.info("Found " + deck.getCardList().size() + " cards");
            allCollectedCard.addAll(deck.getCardList());
        }

        if (treatDecks.size() == 0) {
            log.info(
                    "No deck to treat, exiting were found among the total list (maybe the deck is not legal yet or not public) we will fetch him apart");
            for (String deckId : notBuyedDecks) {
                UserDataDeck deck = new UserDataDeck();
                deck.setPublicId(deckId);

                getDeckListFor(deck);

                log.info("Found " + deck.getCardList().size() + " cards for deck : " + deck.getName());

                deck.setPublicId(deckId);
                treatDecks.add(deck);
                log.info("Adding deck " + deck.getName() + " to the list of decks to treat");
            }

        }

        for (UserDataDeck treatDeck : treatDecks) {

            log.info("Now parsing the cards to buy and to proxy for deck : " + treatDeck.getName());

            int nProx = 0;
            int nMaybe = 0;
            int nBuy = 0;
            Double totalUsdProx = 0.0;
            Double totalUsdMaybe = 0.0;
            Double totalUsdBuy = 0.0;
            File currentFolder = new File("OUT/" + treatDeck.getName());
            if (!currentFolder.exists()) {
                currentFolder.mkdirs();
                log.info("Creating folder for deck : " + currentFolder.getPath());
            } else {
                log.info("Folder already exists for deck : " + currentFolder.getPath());
            }
            try (PrintWriter outBuy = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(currentFolder.getAbsolutePath() + "/newToBuyCards.csv"),
                    StandardCharsets.UTF_8))) {

                try (PrintWriter outProx = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(currentFolder.getAbsolutePath() + "/alreadyHaveCards.csv"),
                        StandardCharsets.UTF_8))) {
                    outProx.println("Cardname;FoundInDeck;BoardType");
                    try (PrintWriter sidedCard = new PrintWriter(new OutputStreamWriter(
                            new FileOutputStream(currentFolder.getAbsolutePath() + "/sidedCards.csv"),
                            StandardCharsets.UTF_8))) {
                        sidedCard.println("Cardname;FoundInDeck;BoardType");
                        for (Card card : treatDeck.getCardList()) {
                            if (!card.getTypeBoard().equals("mainboard")) { // we will only buy/proxy mainboard
                                continue;
                            }

                            List<Card> matchingCardMain = findMatchingCard(card, allCollectedCard, "mainboard");
                            List<Card> matchingCardsSide = findMatchingCard(card, allCollectedCard, "sideboard");
                            if (matchingCardsSide.size() == 0 && allowConsiderBoard) {
                                matchingCardsSide = findMatchingCard(card, allCollectedCard, "maybeboard");
                            }
                            if (matchingCardMain.size() > 0 || matchingCardsSide.size() > 0) {
                                if (matchingCardMain.size() > 0) {
                                    outProx.println(
                                            getInfoLine(matchingCardMain));
                                    totalUsdProx += card.getPriceUsd();
                                    nProx++;
                                } else {
                                    sidedCard.println(getInfoLine(matchingCardsSide));
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
            log.info("The deck " + treatDeck.getName() + " contains " + nBuy + " new cards to buy (estimated at "
                    + String.format("%.2f", totalUsdBuy) + " usd) and " + nMaybe
                    + " to maybe proxy (after check in in side or maybeboard)  (economy of "
                    + String.format("%.2f", totalUsdMaybe) + " usd)) and " + nProx + " to proxy (economy of "
                    + String.format("%.2f", totalUsdProx) + " usd)).");

        }
        Double totalCollectUsdSideOnly = 0.0;
        Double totalCollectUsd = 0.0;
        Set<Card> singleCardByNameForPrice = new HashSet<>();
        singleCardByNameForPrice.addAll(allCollectedCard);
        for (Card card : singleCardByNameForPrice) {
            if (card.getTypeBoard().equalsIgnoreCase("sideboard")
                    || card.getTypeBoard().equalsIgnoreCase("mainboard")) {
                totalCollectUsdSideOnly += card.getPriceUsd();
            }
            totalCollectUsd += card.getPriceUsd();
        }

        long endTotal = System.currentTimeMillis();
        log.info("MoxfieldProxyBuyComparer ended it took a total time of " + convertTime(endTotal - startTotal));
        log.info("The total collection of " + user + " has " + allCollectedCard.size() + " cards (estimated at "
                + String.format("%.2f", totalCollectUsdSideOnly) + " usd) and " + String.format("%.2f", totalCollectUsd)
                + " if we consider also maybeboard.");
    }

    private static String getInfoLine(List<Card> listCards) {
        return listCards.get(0).getName() + ";" + listCards.stream()
                .map(Card::getDeckName)
                .collect(Collectors.joining(" | ")) + ";"
                + listCards.stream()
                        .map(Card::getTypeBoard)
                        .collect(Collectors.joining(" | "));
    }

    private static List<Card> findMatchingCard(Card targetCard, List<Card> allCollectedCard, String typeboard) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : allCollectedCard) {
            if (card.equals(targetCard) && card.getTypeBoard().equalsIgnoreCase(typeboard)) {
                list.add(card);
            }
        }
        return list;
    }

    private static void setConenction(HttpURLConnection connection) {
        connection.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        // connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
        // connection.setRequestProperty("Accept-Language",
        // "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");

        connection.setRequestProperty("user-agent", decryptedAgent);
    }

    private static List<UserDataDeck> getAllDeckForUser(String user)
            throws MalformedURLException, InterruptedException {

        List<UserDataDeck> allDecks = new ArrayList<>();

        // oold String userUrl = "https://api2.moxfield.com/v2/users/" + user +
        // "/decks";
        // new url ? https://api2.moxfield.com/v2/decks/search?authorUserNames=

        String userUrl = "https://api2.moxfield.com/v2/decks/search?authorUserNames=" + user + "&pageSize=100";
        // note that this route only fetch legal deck (100 card mainboard)

        int totalNPages = addDecksToList(user, userUrl, allDecks);

        if (totalNPages > 1) {
            for (int nPage = 2; nPage <= totalNPages; nPage++) {
                // old userUrl = "https://api2.moxfield.com/v2/users/" + user + "/decks" +
                // "?pageNumber=" + nPage;
                userUrl = "https://api2.moxfield.com/v2/decks/search?authorUserNames=" + user
                        + "&pageSize=100&pageNumber=" + nPage;
            
                addDecksToList(user, userUrl, allDecks);
            }
        }
        return allDecks;
    }

    private static int addDecksToList(String userName, String userUrl, List<UserDataDeck> allDecks)
            throws MalformedURLException, InterruptedException {
        URL url = new URL(userUrl);
        HttpURLConnection connection = null;
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
                        JsonNode createdByUser = node.path("createdByUser");
                        String creator = createdByUser.path("userName").asText();

                        // skip decks not created by the target user
                        if (!userName.equalsIgnoreCase(creator)) {
                            log.info("Skipping deck " + node.path("name").asText() + " created by " + creator);
                            continue;
                        }
                        UserDataDeck child = new UserDataDeck();
                        child.setOwner(user);
                        child.setPublicId(node.path("publicId").asText());
                        child.setName(node.path("name").asText());
                        allUserData.add(child);
                    }
                }

                int totalPages = rootNode.path("totalPages").asInt();
                allDecks.addAll(allUserData);
                return totalPages;
            } catch (Exception e1) {
                log.err("Error reading the user data", e1);
            }
        } catch (Exception e) {
            log.err("Error getting the connection to user data", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            Thread.sleep(1000);
        }
        return 0;
    }

    private static void getDeckListFor(UserDataDeck deck) throws MalformedURLException, InterruptedException {
        // ex https://api.moxfield.com/v2/decks/all/HxV33izihky7KTwjU0ER9w
        String deckUrl = "https://api.moxfield.com/v2/decks/all/" + deck.getPublicId();
        URL url = new URL(deckUrl);
        HttpURLConnection connection = null;

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
                if (deck.getName() == null || deck.getName().isEmpty()) {
                    deck.setName(deckName);
                }

                // Assuming "mainboard" is a direct child of the root node and contains the
                // cards

                List<String> typeBoards = new ArrayList<>(Arrays.asList("mainboard", "sideboard"));
                if (allowConsiderBoard) {
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
                deck.setCardList(deckCards);
            } catch (Exception e1) {
                e1.printStackTrace();
                log.err("Error reading the user data", e1);

            }
        } catch (Exception e) {
            log.err("Error getting the connection to user data", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            Thread.sleep(1000);
        }
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
