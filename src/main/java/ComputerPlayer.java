import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// represents an AI player
public class ComputerPlayer {
    private GameState gamestate;
    private Player player; // the player object that the AI controls
    private int playerNum; // needed for simulations

    private int timeToWin;
    private int timeToLose;

    private final int simAccuracy = 200; // times to do simulation before average

    // constructor
    public ComputerPlayer(GameState gamestatelink, Player tocontrol, int playerNumber) {
        gamestate = gamestatelink;
        player = tocontrol;
        playerNum = playerNumber;
    }

    //should execute move and returns a description of the move
    //TODO currently just prints the move, doesn't actually execute it
    public String doMove(int actionsLeft) {

        // calculate TTW and TTL
        calculateTime();

        // do all possible moves (abstracted)
        ArrayList<Plan> plans = simulateMoves(actionsLeft);

        // pick the best from plans

        ArrayList<Plan> loosingPlans = new ArrayList<>(); // plans where TTW > TTL
        ArrayList<Plan> winningPlans = new ArrayList<>(); // plans where TTW < TTL

        for (Plan plan : plans) {
            // calculate the real new TTW and TTL
            int newTTL = timeToLose + plan.getTTLDelta();
            int newTTW = timeToWin + plan.getTTWDelta();

            // divide up all the plans into two groups:
            // those that have TTW < TTL and those that have TTW >= TTL
            if (newTTW < newTTL) {
                winningPlans.add(plan);
            } else {
                loosingPlans.add(plan);
            }
        }

        // there are two general cases:
        // all plans have TTL < TTW, so all plans are doomed and we just pick which one we try to improve
        // or at least one plan is TTW < TTL so we pick the plan with the best 'cushion'

        String toReturn;

        if (winningPlans.size() == 0) { // we're doomed
            // pick the plan with the best TTL

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

            toReturn = "The best plans are:\n " + currentBest.getDescription() + " -- " + currentBest.getDeltaValue();
            toReturn += "\n " + secondBest.getDescription() + " -- " + secondBest.getDeltaValue();
            toReturn += "\n " + thirdBest.getDescription() + " -- " + thirdBest.getDeltaValue();

        } else { // we have at least one winning plan
            // pick the plan with the largest difference

            Plan currentBest = null;
            int currentBestCushion = -1;

            for (Plan plan : plans) {
                // calculate the real new TTW and TTL
                int newTTL = timeToLose + plan.getTTLDelta();
                int newTTW = timeToWin + plan.getTTWDelta();
                int cushion = newTTL - newTTW;

                if (currentBest == null || cushion > currentBestCushion) {
                    currentBest = plan;
                    currentBestCushion = cushion;
                }
            }

            toReturn = "The best plan is:\n " + currentBest.getDescription() + " -- " + currentBestCushion + " " + currentBest.getDeltaValue();
        }

        return toReturn;
    }

    // calculates and updates TTW and TTL
    private void calculateTime() {
        calculateTTW(gamestate);
        calculateTTL();
    }

    private int calculateTTW(GameState gamestate) {
        //TODO figure out how to priortize the order of the cards
        double blueAVG=0; double blackAVG=0; double yellowAVG=0; double redAVG=0;
        int bTTW=0; int uTTW=0; int yTTW=0; int rTTW=0;

        HashMap<String,Double> averages = gamestate.avgCityTime(player);
        for (Map.Entry<String,Double> num : averages.entrySet()){
            String color = num.getKey();
            Double avg = num.getValue();

            if (color.equals("B")){
                boolean blue = gamestate.isDiseaseCured(color);
                if(blue==true){
                    bTTW=0;
                    System.out.println("Blue is eradicated");
                }
                blueAVG = avg;

            }if (color.equals("R")){
                boolean red = gamestate.isDiseaseCured(color);
                if(red==true){
                    rTTW=0;
                    System.out.println("red is eradicated");
                }
                redAVG = avg;

            }if (color.equals("Y")){
                boolean yellow = gamestate.isDiseaseCured(color);
                if (yellow==true){
                    yTTW=0;
                    System.out.println("yellow is eradicated");
                }
                yellowAVG = avg;

            }if (color.equals("U")){
                boolean black = gamestate.isDiseaseCured(color);
                if (black==true){
                    uTTW=0;
                    System.out.println("Black is eradicated");
                }
                blackAVG = avg;
            }
        }

        int size = player.pathToClosestStation(gamestate).size();
        bTTW = (int) blueAVG + size + 5;
        rTTW = (int) redAVG + size + 6;
        yTTW = (int) yellowAVG + size + 7;
        uTTW = (int) blackAVG + size + 8;

        timeToWin = bTTW + uTTW + rTTW + yTTW;

        return timeToWin;
    }

    private int calculateTTL() {
        // calculates TTL by simulating and counting how many turns it would take to lose if we didn't do anything

        // simulate a bunch of times and average the number
        SimulationGameState sim;
        ArrayList<Integer> toAverage = new ArrayList<>();

        for (int i = 0; i < simAccuracy; i++) { // simulate many times
            sim = new SimulationGameState(gamestate);
            toAverage.add(sim.simulateUntilLoss());
        }

        // average the results
        int simTTL = 0;
        for (int i : toAverage) {
            simTTL += i;
        }
        simTTL = simTTL / toAverage.size();

        timeToLose = simTTL;

        return timeToLose;
    }

    //simulate all possible moves
    private ArrayList<Plan> simulateMoves(int actionsLeft) {
        //possible moves:

        // treat
        ArrayList<Plan> treatPlans = simulateTreat(actionsLeft);

        //TODO discover (+ trade cards)
        //ArrayList<Plan> discoverPlans = simulateDiscover();

        //TODO build research station (skipping)

        ArrayList<Plan> plans = new ArrayList<>();
        plans.addAll(treatPlans);
        //plans.addAll(discoverPlans);

        return plans;
    }

    // return all the possible plans for treating cubes
    private ArrayList<Plan> simulateTreat(int actionsLeftInTurn) {
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
            int cubeCount = cities.get(cityName).getCubeCount(); //TODO cube count is only for the default color - doesn't consider different-color cubes that have spread here

            if (!gamestate.isDiseaseCured(cities.get(cityName).getColor())) { // if that color isn't cured
                //for all possible cubes removed
                for (int i = 1; i <= cubeCount; i++) {

                    SimulationGameState sim;
                    ArrayList<Integer> toAverage = new ArrayList<>(); // list of numbers that we'll average later

                    for (int h = 0; h < simAccuracy; h++) { // simulate many times
                        sim = new SimulationGameState(gamestate);

                        // yay, lambda expressions!
                        SimulationGameState s = sim;
                        Runnable move = () -> s.treatDisease(cityName, cubeCount); // java isn't good for this
                        toAverage.add(sim.simulateUntilLoss(playerNum, path.size() + i, actionsLeftInTurn, move));
                    }

                    // average
                    int newTTL = 0;
                    for (int k : toAverage) {
                        newTTL += k;
                    }
                    newTTL = newTTL / toAverage.size(); // get average

                    TTLDelta = newTTL - timeToLose;

                    plans.add(new Plan("Treat " + i + " cubes at " + cityName, 0, TTLDelta, path));
                }
            } else { // otherwise, that color is cured

                // don't need to loop
                SimulationGameState sim;
                ArrayList<Integer> toAverage = new ArrayList<>();

                for (int h = 0; h < simAccuracy; h++) { // simulate many times
                    sim = new SimulationGameState(gamestate);

                    // similar to the other case
                    SimulationGameState s = sim;
                    Runnable move = () -> s.treatDisease(cityName, 3);
                    toAverage.add(sim.simulateUntilLoss(playerNum, path.size() + 3, actionsLeftInTurn, move));
                }

                // average
                int newTTL = 0;
                for (int k : toAverage) {
                    newTTL += k;
                }
                newTTL = newTTL / toAverage.size();

                TTLDelta = newTTL - timeToLose;

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


            if (count >= 5 && !gamestate.isDiseaseEradicated(color)) {
                ArrayList<String> path = player.pathToClosestStation(gamestate);
                String destination = path.get(path.size() + 1);

                SimulationGameState sim = new SimulationGameState(gamestate);
                ArrayList<PlayerCard> hand = player.getHand();
                String card1=null; String card2 = null; String card3=null; String card4 = null; String card5 = null;
                for (int i = 0; i < hand.size(); i++) {
                    if (hand.get(i).getColor().equals(color)) {
                        if (card1 != null) {
                            card1 = hand.get(i).getCity();
                        } else if (card2 != null) {
                            card2 = hand.get(i).getCity();
                        } else if (card3 != null) {
                            card3 = hand.get(i).getCity();
                        } else if (card4 != null) {
                            card4 = hand.get(i).getCity();
                        } else if (card5!=null){
                            card5 = hand.get(i).getCity();
                        }
                    }
                }
                sim.getPlayers().get(playerNum).discoverCure(card1, card2, card3, card4, card5, sim);

                int newTTW = calculateTTW(sim);

                int TTWDelta = timeToWin - newTTW;

                Plan curePlan = new Plan(("Drive to " + destination + " and cure " + color), TTWDelta, -(path.size() + 1), path);
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

    public int getTimeToWin(){
        calculateTTW(gamestate);

        return timeToWin;
    }

}
