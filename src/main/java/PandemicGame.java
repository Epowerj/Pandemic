
import java.text.DecimalFormat;
import java.util.*;

//Main Game
public class PandemicGame {
    //this is the main
    public static void main(String[] args) {
        System.out.println("- Start -");

        GameState gamestate = new GameState("cities.txt"); // load the data file and create the game state

        gamestate.gameSetup();

        // register two players as computer players
        // allows the use of the 'ai' command
        gamestate.makeAI(0);
        gamestate.makeAI(1);

        ioloop(gamestate); // launch the input - output loop
    }

    private static void ioloop(GameState gamestate) {
        System.out.print("\n\n");
        Scanner reader = new Scanner(System.in);

        String commandlist = "\n{ai, go, info, cubeinfo, deckinfo, drive, directflight, charterflight, shuttleflight, buildstation, treat, share, take, discover} \nYour input: \n";

        ArrayList<Player> players = gamestate.getPlayers();

        printPlayerInfo(gamestate);
        printResearchStations(gamestate);
        printCubeInfo(gamestate);

        boolean looping = true;

        while (looping) { //each turn

            for (int playerNum = 0; playerNum < players.size() && looping; playerNum++) { // for each player

                Player player = players.get(playerNum);

                System.out.println("Its player " + playerNum + "s turn!");

                for (int actionNum = 4; actionNum > 0 && looping; actionNum--) { // 4 actions per player

                    discardExtra(player, playerNum, gamestate, reader);

                    System.out.println("Player " + playerNum + " has " + actionNum + " actions left");

                    // get input
                    System.out.print(commandlist);
                    String line = reader.nextLine().toLowerCase(); // intellij sometimes crashes here for some reason
                    String[] input = line.split(" "); // list of words

                    // quit
                    //TODO doesn't always work
                    if (input[0].equals("exit") || input[0].equals("quit") || input[0].equals("q")) {
                        looping = false;
                    } else {
                        //now do actions
                        boolean success = doMove(input, gamestate, player, actionNum);

                        while (!success) { //keep trying until success
                            //get input again
                            System.out.println("Try again!");
                            System.out.print(commandlist);
                            line = reader.nextLine().toLowerCase();
                            input = line.split(" "); //list of words

                            success = doMove(input, gamestate, player, actionNum);
                        }
                    }

                    discardExtra(player, playerNum, gamestate, reader); // check if player has over 7 cards
                }

                //at the end of each player's moves

                gamestate.newTurn(player); // draw cards, etc

                discardExtra(player, playerNum, gamestate, reader); // check if player has over 7 cards

            }
        }

        System.out.println(" - Done - ");
    }

    // handles commands
    private static boolean doMove(String[] input, GameState gamestate, Player player, int actionNum) {
        String move = input[0];

        boolean success = false;

        // print info
        if (move.equals("info")) {
            printPlayerInfo(gamestate);
            printResearchStations(gamestate);
            printBoardInfo(gamestate);
            gamestate.avgCityTime(player);


          /*  if (input.length > 1) {
                String target = input[1];
                printPrediction(gamestate, target);
            }

            success = false;*/
        }

        // print info on cubes
        if (move.equals("cubeinfo")) {
            printCubeInfo(gamestate);

            success = false;
        }

        // print info on decks
        if (move.equals("deckinfo")) {
            printDeckInfo(gamestate);

            success = false;
        }

        // print the fastest way to drive anywhere
        // deprecated; use 'go' instead
        if (move.equals("godrive")) {
            if (input.length >= 2) {
                String destination = input[1];

                System.out.println(player.goDrivePrint(destination, gamestate));

                success = true;
            } else {
                success = false;
            }

            if (success == false) {
                System.out.println("Bad move");
                System.out.println("Usage: godrive <destination>");
            }

            success = false; //don't want to actually count this though
        }

        // print the fastest way to get to somewhere
        if (move.equals("go")) {
            if (input.length >= 2) {
                String destination = input[1];

                System.out.println(player.goAnyPrint(destination, gamestate));

                success = true;
            } else {
                success = false;
            }

            if (success == false) {
                System.out.println("Bad move");
                System.out.println("Usage: go <destination>");
            }

            success = false; //don't want to actually count this though
        }

        // print the chance of infection
        if (move.equals("infchance")) {
            if (input.length >= 2) {
                String destination = input[1];

                printPrediction(gamestate, destination);

                success = true;
            } else {
                success = false;
            }

            if (success == false) {
                System.out.println("Bad move");
                System.out.println("Usage: infchance <card>");
            }

            success = false; //don't want to actually count this though
        }

        // print the time to lose
        if (move.equals("ttl")) {

            // check if the current player is an ai
            // first find the current player index
            int currentPlayer;
            for (currentPlayer = 0; currentPlayer < gamestate.getPlayers().size(); currentPlayer++) {
                if (gamestate.getPlayers().get(currentPlayer) == player) {
                    break;
                }
            }

            // do the check
            String aiMove;
            if (gamestate.isAI(currentPlayer)) {
                System.out.println("The TTL is: " + gamestate.getAI(currentPlayer).getTimeToLose());
            } else {
                System.out.println("The current player doesn't have an associated AI");
            }

            success = false;
        }

        // print the time to win
        if (move.equals("ttw")){
            // check if the current player is an ai
            // first find the current player index
            int currentPlayer;
            for (currentPlayer = 0; currentPlayer < gamestate.getPlayers().size(); currentPlayer++) {
                if (gamestate.getPlayers().get(currentPlayer) == player) {
                    break;
                }
            }

            // do the check
            String aiMove;
            if (gamestate.isAI(currentPlayer)) {
                System.out.println("The TTW is: " + gamestate.getAI(currentPlayer).getTimeToWin());
            } else {
                System.out.println("The current player doesn't have an associated AI");
            }

            success = false;
        }

        // print what the ai thinks is the best move
        if (move.equals("ai")) {
            // check if the current player is an ai
            // first find the current player index
            int currentPlayer;
            for (currentPlayer = 0; currentPlayer < gamestate.getPlayers().size(); currentPlayer++) {
                if (gamestate.getPlayers().get(currentPlayer) == player) {
                    break;
                }
            }

            // do the check
            String aiMove;
            if (gamestate.isAI(currentPlayer)) {
                aiMove = gamestate.getAI(currentPlayer).doMove(actionNum);
            } else {
                aiMove = "The current player doesn't have an associated AI";
            }

            System.out.println(aiMove);

            success = false;
        }

        if (move.equals("drive")) {
            if (input.length >= 2) {
                String destination = input[1];

                success = player.drive(destination, gamestate);
            } else {
                success = false;
            }

            if (success == false) {
                System.out.println("Bad move");
                System.out.println("Usage: drive <destination>");
            }
        }

        if (move.equals("directflight")) {
            if (input.length >= 2) {
                String destination = input[1];

                success = player.directFlight(destination, gamestate);
            } else {
                success = false;
            }

            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: directflight <destination>");
            }
        }

        if (move.equals("charterflight")) {
            if (input.length >= 2) {
                String destination = input[1];

                success = player.charterFlight(destination, gamestate);
            } else {
                success = false;
            }

            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: charterflight <destination>");
            }
        }

        if (move.equals("shuttleflight")) {
            if (input.length >= 2) {
                String destination = input[1];

                success = player.shuttleFlight(destination, gamestate);
            } else {
                success = false;
            }

            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: shuttleflight <destination>");
            }
        }

        // build a research station at current location
        if (move.equals("buildstation")) {
            success = player.buildResearchStation(gamestate);
            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: buildstation");
            }
        }

        // get rid of one cube at current location
        if (move.equals("treat")) {
            player.treatDisease(gamestate);
            success = true;
        }

        // give a card to another player
        if (move.equals("share")) {
            ArrayList<Player> players = gamestate.getPlayers();

            if (input.length >= 3) {
                String destination = input[1];

                String pnum = input[2];

                success = player.shareKnowledge(players.get(Integer.parseInt(pnum)), destination);
            } else {
                success = false;
            }

            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: share <destination card> <player number>");
            }
        }

        // take a card from another player
        if (move.equals("take")) {

            ArrayList<Player> players = gamestate.getPlayers();

            if (input.length >= 3) {
                String destination = input[1];

                String pnum = input[2];

                success = player.takeKnowledge(players.get(Integer.parseInt(pnum)), destination);
            } else {
                success = false;
            }

            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: take <destination card> <player number>");
            }
        }

        // discover a cure by using 5 cards
        if (move.equals("discover")) {

            if (input.length >= 6) {

                success = player.discoverCure(input[1], input[2], input[3], input[4], input[5], gamestate);
            }

            if (!success) {
                System.out.println("Bad move");
                System.out.println("Usage: discover <card> <card> <card> <card> <card>");
            }
        }

        return success;
    }

    //discard extra cards from a players hand if they have more than 7
    static private void discardExtra(Player player, int playerNum, GameState gamestate, Scanner reader) {
        //if there are more than 7 cards in this player's hand, the user must discard
        if (player.getHand().size() > 7) {
            System.out.println("Player " + playerNum + " has over 7 cards!");

            printPlayerInfo(gamestate);

            while (player.getHand().size() > 7) {
                System.out.println("Need to discard " + (player.getHand().size() - 7) + " cards");

                System.out.println("Which card to discard: ");
                String discardCard = reader.nextLine().toLowerCase();

                if (player.isHoldingCityCard(discardCard)) {
                    player.discardFromHand(discardCard);
                } else {
                    System.out.println("Player " + player + " isn't holding that card!");
                }
            }
        }
    }

    static void printPlayerInfo(GameState gamestate) {
        ArrayList<Player> players = gamestate.getPlayers();

        System.out.println("Player info: ");
        for (int i = 0; i < players.size(); i++) {
            System.out.print("Player " + i + "[" + players.get(i).getCurrentCity() + "] - ");
            System.out.print("Has cards: ");
            for (Card card : players.get(i).getHand()) {
                System.out.print(card.getCardInfoString() + ", ");
            }
            System.out.print("\n");
        }

        System.out.print("\n");
    }

    static void printCubeInfo(GameState gameState) {
        HashMap<String, City> cities = gameState.getCities();

        for (Map.Entry<String, City> entry : cities.entrySet()) {
            String key = entry.getKey();
            City value = entry.getValue();

            HashMap<String, Integer> cubelist = value.getCubeList();

            for (Map.Entry<String, Integer> cubenum : cubelist.entrySet()) {
                String color = cubenum.getKey();
                Integer count = cubenum.getValue();

                if (count > 0) {
                    System.out.println(value.getName() + " has " + count + " " + color + " cubes");
                }
            }
        }

        System.out.print("\n");
    }

    static void printDeckInfo(GameState gameState) {
        ArrayList<Player> players = gameState.getPlayers();
        System.out.println("Player deck: ");
        gameState.getPlayerDeck().printAllCards();
        System.out.println("Infection deck: ");
        gameState.getInfectionDeck().printAllCards();

        System.out.print("\n");
    }

    static void printBoardInfo(GameState gameState) {

        if (gameState.haveLost()) {
            System.out.println("----GAME OVER----");
        }
        System.out.println("Outbreaks: " + gameState.getOutbreak());
        System.out.println("Infection rate: " + gameState.getInfectionRate());
        // probably should print cured status
    }

    static void printResearchStations(GameState gameState) {
        ArrayList<String> stations = gameState.getStations();

        System.out.print("Research stations are located in: ");

        for (String station : stations) {
            System.out.print(station + ", ");
        }

        System.out.print("\n\n");
    }


    static double predictInfection(GameState gameState, String target) {
        //chance that an outbreak might happen by using the shuffle back method
        DecimalFormat f = new DecimalFormat("#.000");

        ArrayList<ArrayList<InfectionCard>> shuffleBacks = gameState.getInfectiondeck().getShuffleBack();
        int infectionrate = gameState.getInfectionRate();

        double result = 0;

        if (!gameState.getInfectiondeck().isInDiscard(target)) {
            if (!shuffleBacks.isEmpty()) {

                ArrayList<InfectionCard> topStack = shuffleBacks.get(shuffleBacks.size() - 1);

                if (infectionrate <= topStack.size()) {
                    boolean contains = false;
                    for (InfectionCard card : topStack) {
                        if (card.getCity().equals(target)) {
                            contains = true;
                        }
                    }
                    if (contains) {
                        result = (1f / topStack.size()) * infectionrate;
                    }
                } else {
                    int topIndex = shuffleBacks.size() - 1;

                    while (infectionrate > topStack.size()) {
                        boolean contains = false;
                        for (InfectionCard card : topStack) {
                            if (card.getCity().equals(target)) {
                                contains = true;
                            }
                        }
                        if (contains) {
                            result = 1;
                            break;
                        } else {
                            infectionrate -= topStack.size();

                            topIndex--;
                            if (topIndex < 0) {
                                result = (1f / gameState.getInfectionSize()) * infectionrate;
                            } else {

                                topStack = shuffleBacks.get(topIndex);
                            }
                        }
                    }
                    if (result == 0) {
                        boolean contains = false;
                        for (InfectionCard card : topStack) {
                            if (card.getCity().equals(target)) {
                                contains = true;
                            }
                        }
                        if (contains) {
                            result = (1f / topStack.size()) * infectionrate;
                        }
                    }
                }
            } else {
                result = (1f / gameState.getInfectionSize()) * infectionrate;
            }
        } else {
            result = 0;
        }
        result =result * 100f;
        return result;
    }

    //This is a printer for the probability of predict Infection used for testing
    static void printPrediction(GameState gameState, String goal){
        double result = predictInfection(gameState,goal);
        DecimalFormat f = new DecimalFormat("#.000");
        System.out.println("The chance of drawing that card is: " + f.format(result) + "%");

    }

    static void congestedCities(GameState gameState) {
        /*
        Takes in the list of cities and the cube list and checks
        which cities have 3 or more cubes this value can be changed
         */
        ArrayList<Double> predictions = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        HashMap<String,City> cities = gameState.getCities();

        for (Map.Entry<String, City> entry : cities.entrySet()) {
            String key = entry.getKey();
            City value = entry.getValue();

            HashMap<String, Integer> cubelist = value.getCubeList();
            for (Map.Entry<String, Integer> cubenum : cubelist.entrySet()){
                String color = cubenum.getKey();
                Integer count = cubenum.getValue();
                if (count > 2){
                    predictions.add(predictInfection(gameState,key));
                    names.add(key);
                }
            }
        }
        Collections.sort(predictions);
            System.out.println("Possible outbreak: " + names + " " + predictions);
    }

    static void predictEpidemic(GameState gameState) {
        DecimalFormat f = new DecimalFormat("#.000");
        int deckSize = gameState.getPlayerDeck().deckSize();
        int currentEpoch = 1;
        int currentEpochSize = gameState.getEpochOverflow() + 1;
        double predictor=0;

        while (deckSize > currentEpochSize) {
            //this isn't the epoch we're lookinge for; increment

            currentEpoch++;
            deckSize -= currentEpochSize;
            currentEpochSize = gameState.getEpochSize() + 1;
        }

        //now we know which epoch we're in
        boolean isEpidemicDrawn = (gameState.getInfectionrateindex() > (gameState.getEpidemicDifficulty() - currentEpoch));
        int cardsLeft = deckSize;

        if ( isEpidemicDrawn==false){
            predictor = (2.0/cardsLeft);
        } else {
            predictor = 0.0;
        }

        System.out.println("The chance of epidemic is: " + f.format(predictor));
        System.out.println("Is epidemic drawn: " + isEpidemicDrawn);
        System.out.println("How many cards left: " + cardsLeft);
    }

    static void printPlayerPrediction(GameState gameState){
        /* This will print the player prediction method
        it tells you which color card might show up the next play
         */
        DecimalFormat df = new DecimalFormat("#.00");
        HashMap<String,Double> predictions = gameState.predictPlayer();
        double u=0; double r=0; double y=0; double b=0;
        for (Map.Entry<String,Double> entries : predictions.entrySet()){
            String color = entries.getKey();
            Double num = entries.getValue();
            if (color.equals("B")){ b = num;}
            if(color.equals("R")){ r = num;}
            if(color.equals("U")){ u = num;}
            if(color.equals("Y")){ y = num;}
        }
        System.out.println("Chance of black: " + df.format(u) + "%");
        System.out.println("Chance of red: " + df.format(r) + "%");
        System.out.println("Chance of yellow: " + df.format(y) + "%");
        System.out.println("Chance of blue: " + df.format(b) + "%");

    }

}

