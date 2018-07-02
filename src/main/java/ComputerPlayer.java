import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ComputerPlayer {
    private GameState gamestate;
    private Player player;

    private int timeToWin;
    private int timeToLose;

    private final int simAccuracy = 100; // times to do simulation before average

    public ComputerPlayer(GameState gamestatelink, Player tocontrol) {
        gamestate = gamestatelink;
        player = tocontrol;
    }

    //should do move and returns a description of the move
    //TODO currently just prints
    public String doMove() {

        // calculate TTW and TTL
        calculateTime();

        // do all possible moves (abstracted)
        ArrayList<Plan> plans = simulateMoves();

        // pick the best from plans
        Plan currentBest = null;
        Plan secondBest = null;
        Plan thirdBest = null;
        for (Plan plan : plans) { //get the top 3 plans
            if (currentBest == null || plan.getDeltaValue() > currentBest.getDeltaValue()) {
                currentBest = plan;
            } else if (secondBest == null || plan.getDeltaValue() > secondBest.getDeltaValue()) {
                secondBest = plan;
            } else if (thirdBest == null || plan.getDeltaValue() > thirdBest.getDeltaValue()) {
                thirdBest = plan;
            }
        }

        String toReturn = "The best plans are:\n " + currentBest.getDescription() + " -- " + currentBest.getDeltaValue();
        toReturn += "\n " + secondBest.getDescription() + " -- " + secondBest.getDeltaValue();
        toReturn += "\n " + thirdBest.getDescription() + " -- " + thirdBest.getDeltaValue();

        return toReturn;
    }

    private void calculateTime() {
        calculateTTW();
        calculateTTL();
    }

    private int calculateTTW() {
      //TODO figure out how to priortize the order of the cards
        HashMap<String,Double> averages = gamestate.avgCityTime(player);
        for (Map.Entry<String,Double> num : averages.entrySet()){
            String color = num.getKey();
            Double avg = num.getValue();
            if (color.equals("B")){

            }if (color.equals("R")){

            }if (color.equals("Y")){

            }if (color.equals("U")){

            }
        }

        int size = player.pathToClosestStation(gamestate).size();
        //ArrayList<PlayerCard> hand =  player.getHand();
        int avg=0; int num=0; int exchange=0;

      /*  for (PlayerCard card : hand){
           String city =  card.getCity();
           String current = player.getCurrentCity();
           num = player.goNormal(current,city,gamestate).size();
           exchange++;

        }*/

       // avg = num/hand.size();

        //timeToWin = (size + avg + exchange) * 4;

        return timeToWin;
    }

    private int calculateTTL() {

        // old
        //int cardsTTL = playerCardsTTL();
        // calculate out of cubes TTL
        // calculate 8 outbreaks TTL
        // return the smallest

        // simulate a bunch of times and average the number
        SimulationGameState sim;
        ArrayList<Integer> toAverage = new ArrayList<>();

        for (int i = 0; i < simAccuracy; i++) { // simulate many times
            sim = new SimulationGameState(gamestate);
            toAverage.add(sim.simulateUntilLoss());
        }

        // average
        int simTTL = 0;
        for (int i : toAverage) {
            simTTL += i;
        }
        simTTL = simTTL / toAverage.size();

        timeToLose = simTTL;

        return timeToLose;
    }

    private int playerCardsTTL() {
        return gamestate.getPlayerDeck().deckSize() * 2; // how many actions till we run out of player cards
    }

    //simulate all possible moves
    private ArrayList<Plan> simulateMoves() {
        //possible moves:

        // treat
        ArrayList<Plan> treatPlans = simulateTreat();

        // discover (+ trade cards)
        //TODO ArrayList<Plan> discoverPlans = simulateDiscover();

        //TODO build research station (skipping)

        ArrayList<Plan> plans = new ArrayList<>();
        plans.addAll(treatPlans);
        //plans.addAll(discoverPlans);

        return plans;
    }

    private ArrayList<Plan> simulateTreat() {
        //have to list all the possible POIs and go to them

        HashMap<String, City> cities = gamestate.getCities();
        ArrayList<String> allPOIs = new ArrayList<>(); //list of nodes that have cubes

        //build the list of cities that have cubes
        for (Map.Entry<String, City> entry : cities.entrySet()) {
            String cityName = entry.getKey();
            City city = entry.getValue();

            if (city.getCubeCount() > 0) {
                allPOIs.add(cityName);
            }
        }

        ArrayList<Plan> plans = new ArrayList<>();

        for (String cityName : allPOIs) {
            int TTLDelta = 0;

            //find the turns needed to get there (decrease in TTL)
            ArrayList<String> path = player.goNormal(player.getCurrentCity(), cityName, gamestate);

            //find how it changes the TTL (increase in TTL)

            //get the amount of cubes
            int cubeCount = cities.get(cityName).getCubeCount(); //TODO cube count is only for the default color

            if (!gamestate.isDiseaseCured(cities.get(cityName).getColor())) { // if that color isn't cured
                //for all possible cubes removed
                for (int i = 1; i <= cubeCount; i++) {

                    SimulationGameState sim;
                    ArrayList<Integer> toAverage = new ArrayList<>(); // list of numbers that we'll average later

                    for (int h = 0; h < simAccuracy; h++) { // simulate many times
                        sim = new SimulationGameState(gamestate);
                        sim.treatDisease(cityName, cubeCount);
                        toAverage.add(sim.simulateUntilLoss());
                    }

                    // average
                    int newTTL = 0;
                    for (int k : toAverage) {
                        newTTL += k;
                    }
                    newTTL = newTTL / toAverage.size(); // get average

                    TTLDelta = (newTTL - timeToLose) - path.size() - i;

                    plans.add(new Plan("Treat " + i + " cubes at " + cityName, 0, TTLDelta, path));
                }
            } else { // otherwise, that color is cured

                // don't need to loop
                SimulationGameState sim;
                ArrayList<Integer> toAverage = new ArrayList<>();

                for (int h = 0; h < simAccuracy; h++) { // simulate many times
                    sim = new SimulationGameState(gamestate);
                    sim.treatDisease(cityName, 3);
                    toAverage.add(sim.simulateUntilLoss());
                }

                // average
                int newTTL = 0;
                for (int k : toAverage) {
                    newTTL += k;
                }
                newTTL = newTTL / toAverage.size();

                TTLDelta = (newTTL - timeToLose) - path.size() - 3;

                plans.add(new Plan("Treat all cubes at " + cityName, 0, TTLDelta, path));
            }
        }

        return plans;
    }

    private ArrayList<Plan> simulateDiscover() {
        ArrayList<Plan> plans = new ArrayList<>();

        // if have 5 cards of the same color, can try going for cure

        // check if player has the right cards for curing
        HashMap<String, Integer> cardCount = player.colorCount();
        for (Map.Entry<String, Integer> entry : cardCount.entrySet()) {
            String color = entry.getKey();
            int count = entry.getValue();

            if (count >= 5) {
                //TODO try making a plan to cure this and add it to plans
                ArrayList<String> path = player.pathToClosestStation(gamestate);
                String destination = path.get(path.size() - 1);

                Plan curePlan = new Plan(("Drive to " + destination + " and cure " + color), -20, -(path.size() + 1), path);
                plans.add(curePlan);
            }
        }

        // can always try trading cards??
        //TODO do trades and add generated plans

        return plans;
    }

    public int getTimeToLose() {
        calculateTTL();
        return timeToLose;
    }

    //TODO simulate an exchange and a meet


}
