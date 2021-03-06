import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Player {

    ArrayList<PlayerCard> hand = new ArrayList<>();
    String currentCity = "atlanta"; //default start location
    Role role;

    public Player(Role playerole) {
        role = playerole;
    }

    // copy constructor
    public Player(Player other) {
        // copy hand
        for (int i = 0; i < other.getHand().size(); i++) {
            hand.add(i, other.getHand().get(i));
        }

        currentCity = other.getCurrentCity();
        role = other.getRole();
    }

    public Role getRole() {
        return role;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    //not an action - used only for dispatcherTeleport and manually setting position
    public void setCurrentCity(String toSet) {
        currentCity = toSet;
    }

    public ArrayList<PlayerCard> getHand() {
        return hand;
    }

    private PlayerCard getCityCardFromHand(String city) {
        for (PlayerCard card : hand) {
            if (card.getCity().equals(city)) {
                return card;
            }
        }

        return null;
    }

    public boolean isHoldingCityCard(String target) {

        for (PlayerCard card : hand) {
            if (card.getCity().toLowerCase().equals(target)) {
                return true;
            }
        }

        return false;
    }

    //returns the card and removes it from hand
    private PlayerCard takeCityCard(String targetCity) {
        PlayerCard toReturn = null;

        for (int i = 0; i < hand.size(); i++) {
            PlayerCard card = hand.get(i);

            if (card.getCity().toLowerCase().equals(targetCity)) {
                toReturn = card;
                hand.remove(i);
            }
        }

        return toReturn;
    }

    public void addCardToHand(PlayerCard card) {
        hand.add(card);
    }

    //move to an adjacent city
    //returns false if move isn't possible
    public boolean drive(String destination, GameState gameState) {
        HashMap<String, City> cities = gameState.getCities();

        if (cities.get(currentCity).isAdjacent(destination)) {
            currentCity = destination; //do the move
            return true;
        } else {
            return false;
        }
    }

    //move to a city of a held card
    //returns false if move isn't possible
    public boolean directFlight(String destination, GameState gameState) {
        if (this.isHoldingCityCard(destination)) {
            currentCity = destination; //do the move
            PlayerCard toDiscard = takeCityCard(destination);
            gameState.discardPlayerCard(toDiscard);
            return true;
        } else {
            return false;
        }
    }

    //move to any city if card matches current position
    //returns false if move isn't possible
    public boolean charterFlight(String destination, GameState gameState) {
        if (this.isHoldingCityCard(currentCity)) {
            String previousCity = currentCity;
            currentCity = destination; //do the move
            PlayerCard toDiscard = takeCityCard(previousCity);
            gameState.discardPlayerCard(toDiscard);
            return true;
        } else {
            return false;
        }
    }

    //move between research station cities
    //returns false if move isn't possible
    public boolean shuttleFlight(String destination, GameState gameState) {
        //if current location and destination both have research stations
        if (gameState.cityHasResearchStation(currentCity) && gameState.cityHasResearchStation(destination)) {
            currentCity = destination; //do the move
            return true;
        } else {
            return false;
        }
    }

    //build research station at current location
    //returns false if not possible
    public boolean buildResearchStation(GameState gameState) {
        if (role == Role.OPERATION) { //if the player is an operations expert
            gameState.placeResearchStation(currentCity);

            return true;
        } else if (this.isHoldingCityCard(currentCity)) { //if the player has a card of their current position
            gameState.placeResearchStation(currentCity);

            PlayerCard toDiscard = takeCityCard(currentCity);
            gameState.discardPlayerCard(toDiscard);

            return true;
        } else {
            return false;
        }
    }

    //destroy a disease cube at the current position
    //TODO removing cured disease is an automatic and free action for medic
    public void treatDisease(GameState gameState) {
        City targetCity = gameState.getCities().get(currentCity);

        //if disease is cured or if player is a medic
        if (gameState.isDiseaseCured(targetCity.getColor()) || role == Role.MEDIC) {
            targetCity.removeCubes(targetCity.getCubeCount()); //TODO only works on the cubes of the same color as city
        } else {
            targetCity.removeCubes(1);
        }
    }

    //share knowledge with another player
    //returns false if it isn't possible
    public boolean shareKnowledge(Player targetPlayer, String targetCity) {

        boolean isResearcher = (role == Role.RESEARCHER);
        boolean inTargetCity = (targetCity.equals(currentCity));
        boolean playersShareCity = (targetPlayer.getCurrentCity().equals(currentCity));
        boolean haveTargetCityCard = (isHoldingCityCard(targetCity));

        if (haveTargetCityCard && playersShareCity && (isResearcher || inTargetCity)) {
            //note: target player has to throw out a card if they have too many

            PlayerCard card = takeCityCard(targetCity);
            targetPlayer.addCardToHand(card);

            return true;
        } else {
            return false;
        }
    }

    //take knowledge from another player
    //returns false if it isn't possible
    public boolean takeKnowledge(Player targetPlayer, String targetCity) {

        boolean isResearcher = (targetPlayer.getRole() == Role.RESEARCHER);
        boolean inTargetCity = (targetCity.equals(currentCity));
        boolean playersShareCity = (targetPlayer.getCurrentCity().equals(currentCity));
        boolean haveTargetCityCard = (targetPlayer.isHoldingCityCard(targetCity));

        if (haveTargetCityCard && playersShareCity && (isResearcher || inTargetCity)) {
            //note: target player has to throw out a card if they have too many

            PlayerCard card = targetPlayer.takeCityCard(targetCity);
            addCardToHand(card);

            return true;
        } else {
            return false;
        }
    }

    //cure a disease by sacrificing 5 cards of the same color at a research station
    //if player is a scientist, only 4 cards are needed, the last argument can be any string
    //returns false if it's not possible
    public boolean discoverCure(String cardCity1, String cardCity2, String cardCity3, String cardCity4, String cardCity5, GameState gameState) {

        boolean haveCards = true;
        String color = "";

        if (!isHoldingCityCard(cardCity1)) {
            haveCards = false;
        } else {
            color = getCityCardFromHand(cardCity1).getColor(); //get color
        }

        if (!isHoldingCityCard(cardCity2) && getCityCardFromHand(cardCity2).getColor().equals(color)) {
            haveCards = false;
        }

        if (!isHoldingCityCard(cardCity3) && getCityCardFromHand(cardCity3).getColor().equals(color)) {
            haveCards = false;
        }

        if (!isHoldingCityCard(cardCity4) && getCityCardFromHand(cardCity4).getColor().equals(color)) {
            haveCards = false;
        }

        //scientist doesn't need a fifth card
        if (role != Role.SCIENTIST) {
            if (!isHoldingCityCard(cardCity5) && getCityCardFromHand(cardCity5).getColor().equals(color)) {
                haveCards = false;
            }
        }

        if (gameState.cityHasResearchStation(currentCity) && haveCards) {
            gameState.setCured(color);

            //discard cards
            PlayerCard toDiscard = takeCityCard(cardCity1);
            gameState.discardPlayerCard(toDiscard);
            toDiscard = takeCityCard(cardCity2);
            gameState.discardPlayerCard(toDiscard);
            toDiscard = takeCityCard(cardCity3);
            gameState.discardPlayerCard(toDiscard);
            toDiscard = takeCityCard(cardCity4);
            gameState.discardPlayerCard(toDiscard);
            if (role != Role.SCIENTIST) { //Don't discard the last card if you don't need to
                toDiscard = takeCityCard(cardCity5);
                gameState.discardPlayerCard(toDiscard);
            }

            return true;
        } else {
            return false;
        }
    }

    //move from a research station to any city by discarding any city card
    //only operations expert can do this
    //returns false if not possible
    //TODO only possible once per turn
    public boolean operationsMove(String destination, String cityCardToDiscard, GameState gameState) {

        if (role == Role.OPERATION && gameState.cityHasResearchStation(currentCity)) {
            currentCity = destination; //do the move
            PlayerCard toDiscard = takeCityCard(cityCardToDiscard);
            gameState.discardPlayerCard(toDiscard);

            return true;
        } else {
            return false;
        }
    }

    //move another player
    //only dispatcher can do this
    //returns false if not possible
    public boolean dispatcherMove(Player targetPlayer, String moveType, String destination, GameState gameState) {

        if (role == Role.DISPATCHER) {
            if (moveType.equals("drive")) {
                return targetPlayer.drive(destination, gameState);
            } else if (moveType.equals("directflight")) {
                return targetPlayer.directFlight(destination, gameState);
            } else if (moveType.equals("charterflight")) {
                return targetPlayer.charterFlight(destination, gameState);
            } else if (moveType.equals("shuttleflight")) {
                return targetPlayer.shuttleFlight(destination, gameState);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    //move any player to any other player
    //only Dispatcher can do this
    //returns false if not possible
    public boolean dispatcherTeleport(Player tomove, Player target) {

        if (role == Role.DISPATCHER) {
            tomove.setCurrentCity(target.getCurrentCity());

            return true;
        } else {
            return false;
        }
    }

    public void discardFromHand(String card) {
        Card todiscard = null;

        for (PlayerCard c : hand) {
            if (c.getCity().equals(card)) {
                todiscard = c;
            }
        }

        hand.remove(todiscard);
    }

    //prints the fastest path to drive to destination
    public String goDrivePrint(String destination, GameState gameState) {
        HashMap<String, City> cities = gameState.getCities();

        LinkedList<String> queue = new LinkedList<>(); //for some reason LinkedList is a queue
        ArrayList<String> visited = new ArrayList<>();
        HashMap<String, String> meta = new HashMap<>();

        String root = currentCity.toLowerCase();
        meta.put(root, "");
        queue.add(root);

        while (!queue.isEmpty()) {
            String subRoot = queue.poll();

            if (subRoot.equals(destination)) {
                return goDrivePrintConstruct(subRoot, meta); //return
            } else {
                City subRootCity = cities.get(subRoot);

                for (String child : subRootCity.getAdjacent()) {

                    if (!visited.contains(child)) {
                        meta.put(child, subRoot);
                        queue.add(child);
                        visited.add(child);
                    }
                }

                visited.add(subRoot);
            }
        }

        return "Drive failed";
    }

    //TODO specialist stops cube updates

    //helper method for godriveprint
    private String goDrivePrintConstruct(String state, HashMap<String, String> meta) {
        ArrayList<String> actionList = new ArrayList<>();
        String destination = state;
        String result = "";

        while (!meta.get(state).equals("")) { //until root node
            String action = meta.get(state);
            actionList.add(action);
            state = action;
        }

        ArrayList<String> toReturn = new ArrayList<>();

        //reverse the list
        for (int i = (actionList.size() - 1); i >= 0; i--) {
            //toReturn.add(actionList.get(i));

            result += (actionList.get(i) + " -> "); //append
        }

        result += (destination);

        return result;
    }

    // figures out the fastest path to drive to destination
    public ArrayList<String> goNormal(String start, String destination, GameState gameState) {
        HashMap<String, City> cities = gameState.getCities();

        LinkedList<String> queue = new LinkedList<>(); //for some reason LinkedList is a queue
        ArrayList<String> visited = new ArrayList<>();
        HashMap<String, String> meta = new HashMap<>();

        String root = start.toLowerCase();
        meta.put(root, "");
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            String subRoot = queue.poll();

            if (subRoot.equals(destination)) {
                return goNormalConstruct(subRoot, meta); //return
            } else {
                City subRootCity = cities.get(subRoot);

                for (String child : subRootCity.getAdjacent()) {

                    if (!visited.contains(child)) {
                        meta.put(child, subRoot);
                        queue.add(child);
                        visited.add(child);
                    }
                }

                //shuttle flights
                if (gameState.getStations().contains(subRootCity.getName())) { //if there is a station at current position
                    for (String city : gameState.getStations()) {

                        if (!visited.contains(city)) { //shuttle flights
                            meta.put(city, subRoot);
                            queue.add(city);
                            visited.add(city);
                        }
                    }
                }

                visited.add(subRoot);
            }
        }

        return null;
    }

    // helper function for goAny
    private ArrayList<String> goNormalConstruct(String state, HashMap<String, String> meta) {
        String destination = state;

        ArrayList<String> actionList = new ArrayList<>();
        ArrayList<String> resultList = new ArrayList<>();

        //follow meta to get path
        while (!meta.get(state).equals("")) { //until root node
            String action = meta.get(state);
            actionList.add(action);
            state = action;
        }

        //reverse the list
        //skips the starting node
        for (int i = (actionList.size() - 2); i >= 0; i--) {
            resultList.add(actionList.get(i));
        }

        resultList.add(destination); //the final destination isn't added normally

        return resultList;
    }

    //prints the fastest path to get to destination with and without using cards
    public String goAnyPrint(String destination, GameState gameState) {
        if (!gameState.getCities().containsKey(destination)) {
            return "Invalid city";
        }

        HashMap<String, ArrayList<ArrayList<String>>> results = new HashMap<>(); //card -> 2 results

        for (PlayerCard card : hand) {
            String cardCity = card.getCity();

            ArrayList<ArrayList<String>> toSave = new ArrayList<>();

            //drive to C and then charter flight to B
            ArrayList<String> aDriveCFlyB = goNormal(currentCity, cardCity, gameState);
            //afterwards fly is one move
            aDriveCFlyB.add("flight to " + destination);
            toSave.add(aDriveCFlyB);

            //fly to C and then drive to B
            //fly is one move
            ArrayList<String> aFlyCDriveB = new ArrayList<>();
            aFlyCDriveB.add("flight to " + cardCity);
            toSave.add(aFlyCDriveB);

            ArrayList<String> temp = goNormal(cardCity, destination, gameState);

            //append the drive path to the existing list
            for (int i = 0; i < temp.size(); i++) {
                aFlyCDriveB.add(temp.get(i));
            }

            //fly to C, drive to D, then fly to B
            //Find the shortest for this type of path
            int shortestacdb = Integer.MAX_VALUE;
            //ArrayList shortest = new ArrayList();

            for (PlayerCard step : hand) {
                String stepCity = step.getCity();

                if (!step.equals(card) && !results.containsKey(stepCity)) {
                    ArrayList<String> stepPath = goNormal(cardCity, stepCity, gameState);

                    int aFlyCDriveDFlyB = 1 + stepPath.size() + 1;

                    if (aFlyCDriveDFlyB < shortestacdb) {
                        stepPath.add(0, "flight to " + cardCity);
                        stepPath.add("flight to " + destination);

                        if (toSave.size() >= 3) {
                            toSave.set(2, stepPath);
                        } else {
                            toSave.add(stepPath);
                        }
                    }
                }
            }

            results.put(cardCity, toSave);
        }

        //now get the top n from the list of results
        ArrayList<ArrayList<String>> shortestPaths = new ArrayList<>();
        ArrayList<Integer> shortest = new ArrayList<>();
        final int n = 5; //the top n best paths are printed

        //initialize the array
        for (int i = 0; i < n; i++) {
            shortest.add(Integer.MAX_VALUE);
        }

        //find the best result
        for (Map.Entry<String, ArrayList<ArrayList<String>>> entry : results.entrySet()) {
            String card = entry.getKey();
            ArrayList<ArrayList<String>> result = entry.getValue();

            int aDriveCFlyB = result.get(0).size();
            int aFlyCDriveB = result.get(1).size();
            int aFlyCDriveDFlyB = Integer.MAX_VALUE;

            if (result.size() >= 3) {
                aFlyCDriveDFlyB = result.get(2).size();
            }

            if (aFlyCDriveDFlyB < aDriveCFlyB && aFlyCDriveDFlyB < aFlyCDriveB) {
                for (int i = 0; i < n; i++) {
                    if (aFlyCDriveDFlyB < shortest.get(i)) {
                        shortest.add(i, aFlyCDriveDFlyB);
                        shortestPaths.add(i, result.get(2));

                        break;
                    }
                }
            } else {
                if (aDriveCFlyB < aFlyCDriveB) {

                    for (int i = 0; i < n; i++) {
                        if (aDriveCFlyB < shortest.get(i)) {
                            shortest.add(i, aDriveCFlyB);
                            shortestPaths.add(i, result.get(0));

                            break;
                        }
                    }
                } else {

                    for (int i = 0; i < n; i++) {
                        if (aFlyCDriveB < shortest.get(i)) {
                            shortest.add(i, aDriveCFlyB);
                            shortestPaths.add(i, result.get(1));

                            break;
                        }
                    }
                }
            }
        }

        String toReturn = "";

        ArrayList<String> drive = goNormal(currentCity, destination, gameState);

        toReturn += "Normal drive path: ";

        for (String move : drive) {
            toReturn += "->" + move;
        }
        toReturn += "\n";

        int ncounter = 0; //for printing only the top n
        for (ArrayList<String> path : shortestPaths) {
            if (ncounter < n) { //only print the top n
                for (String move : path) {
                    toReturn += "->" + move;
                }

                toReturn += "\n";

                ncounter++;
            }
        }

        return toReturn;
    }

    // returns the list of card colors in the player's hand
    public HashMap<String, Integer> colorCount() {
        HashMap<String, Integer> colorCount = new HashMap<>();
        colorCount.put("U", 0);
        colorCount.put("B", 0);
        colorCount.put("R", 0);
        colorCount.put("Y", 0);

        for (PlayerCard c : hand) {
            if (c.getColor().equals("U")) {
                colorCount.put("U", colorCount.get("U") + 1);
            } else if (c.getColor().equals("B")) {
                colorCount.put("B", colorCount.get("B") + 1);
            } else if (c.getColor().equals("R")) {
                colorCount.put("R", colorCount.get("R") + 1);
            } else {
                colorCount.put("Y", colorCount.get("Y") + 1);
            }
        }
        return colorCount;

    }

    public ArrayList<String> pathToClosestStation(GameState gameState) {
        ArrayList<ArrayList<String>> paths = new ArrayList<>();
        for (String city : gameState.getStations()) {
            paths.add(goNormal(currentCity, city, gameState));
        }

        int bestIndex = -1;
        int currentShortest = Integer.MAX_VALUE;

        for (int i = 0; i < paths.size(); i++) {

            if (paths.get(i).size() < currentShortest) {
                bestIndex = i;
                currentShortest = paths.get(i).size();
            }
        }

        return paths.get(bestIndex);
    }

    enum Role {OPERATION, MEDIC, PLANNER, DISPATCHER, SPECIALIST, RESEARCHER, SCIENTIST}

}
