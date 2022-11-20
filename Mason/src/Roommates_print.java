import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Interval;
import java.io.*;

import java.util.Arrays;
import java.util.Collections;

public class Roommates_print extends SimState
{
    public int numRoommates = 1000; // Sample 1000 roommate agents in total.
    public Bag allRoommates = new Bag(); // All roommate agents are stored here.
    public Roommates_print(long seed)
    {
        super(seed);
    }
    public int temp_num_for_output = 0;
    public double[][] utility_record = new double[numRoommates][2000]; // Utility record for ELESSAR strategy
    public double[][] utility_record_primary = new double[numRoommates][2000]; // Utility record for primary strategy
    public double[][] utility_record_conservative = new double[numRoommates][2000]; // Utility record for conservative strategy

    public double[] utility_distri = new double[10000]; // Check utility distribution
    public int distri_num = 0;

    public void start(){
        super.start();

        // Create new roommates
        for(int i = 0; i < numRoommates; i++) {
            Roommate_print roommate_print = new Roommate_print();

            // Define Dorms;
            roommate_print.dormNum = (int)(i/2);
            allRoommates.add(roommate_print);

            // Define Threshold for later usage;
            double threshold_1_outof_4 = 0.75;
            double threshold_2_outof_4 = 0.5;
            double threshold_3_outof_4 = 0.25;

            double threshold_1_outof_5 = 0.8;
            double threshold_2_outof_5 = 0.6;
            double threshold_3_outof_5 = 0.4;
            double threshold_4_outof_5 = 0.2;

            double threshold_1_outof_6 = 5.0/6.0;
            double threshold_2_outof_6 = 4.0/6.0;
            double threshold_3_outof_6 = 3.0/6.0;
            double threshold_4_outof_6 = 2.0/6.0;
            double threshold_5_outof_6 = 1.0/6.0;

            // Initialize each agent's norm:
            // Each agent prohibits a certain activity at a certain time point.
            // 1. Randomly select a time point for this agent's norm:
            double rand_num_norm = random.nextDouble();
            if (rand_num_norm > threshold_1_outof_5){
                roommate_print.norm_TimePoint = 0;
            } else if (rand_num_norm > threshold_2_outof_5) {
                roommate_print.norm_TimePoint = 1;
            } else if (rand_num_norm > threshold_3_outof_5) {
                roommate_print.norm_TimePoint = 2;
            } else if (rand_num_norm > threshold_4_outof_5) {
                roommate_print.norm_TimePoint = 3;
            } else {
                roommate_print.norm_TimePoint = 4;
            }

            // 2. Randomly select an activity for this agent's norm:
            rand_num_norm = random.nextDouble();
            if (rand_num_norm > threshold_1_outof_4){
                roommate_print.norm_Activity = 0;
            } else if (rand_num_norm > threshold_1_outof_4) {
                roommate_print.norm_Activity = 1;
            } else if (rand_num_norm > threshold_1_outof_4) {
                roommate_print.norm_Activity = 2;
            } else {
                roommate_print.norm_Activity = 3;
            }

            // Initialize the action-value table for this roommate:
            for (int m=0; m<5; m++){
                for (int n=0; n<4; n++){
                    for (int l=0; l<4; l++){
                        roommate_print.action_value[m][n][l] = 0.0;
                    }
                }
            }

            // Initialize the value_weight array for this roommate:
            for (int m=0; m<5; m++){
                for (int n=0; n<4; n++){
                    roommate_print.value_weights[m][n] = 1;
                }
            }

            // For each time point (5 possible time points in total)
            for (int t=0; t<5; t++){
                // For each value (4 possible values in total)
                // Set the value_weights array.
                double rand_num = random.nextDouble();
                if (rand_num > 0.5){ // With probability 0.5, the agent have one favorite value at a certain time point.
                    double rand_num_ = random.nextDouble();
                    if (rand_num_ > threshold_1_outof_4){
                        roommate_print.value_weights[t][0] = 4;
                    } else if (rand_num_ > threshold_2_outof_4) {
                        roommate_print.value_weights[t][1] = 4;
                    } else if (rand_num_ > threshold_3_outof_4) {
                        roommate_print.value_weights[t][2] = 4;
                    } else {
                        roommate_print.value_weights[t][3] = 4;
                    }
                } else { // With probability 0.5, the agent have two equally favorite value at a certain time point.
                    double rand_num_ = random.nextDouble();
                    if (rand_num_ > threshold_1_outof_6){
                        roommate_print.value_weights[t][0] = 2;
                        roommate_print.value_weights[t][1] = 2;
                    } else if (rand_num_ > threshold_2_outof_6) {
                        roommate_print.value_weights[t][0] = 2;
                        roommate_print.value_weights[t][2] = 2;
                    } else if (rand_num_ > threshold_3_outof_6) {
                        roommate_print.value_weights[t][0] = 2;
                        roommate_print.value_weights[t][3] = 2;
                    } else if (rand_num_ > threshold_4_outof_6) {
                        roommate_print.value_weights[t][1] = 2;
                        roommate_print.value_weights[t][2] = 2;
                    } else if (rand_num_ > threshold_5_outof_6) {
                        roommate_print.value_weights[t][1] = 2;
                        roommate_print.value_weights[t][3] = 2;
                    } else {
                        roommate_print.value_weights[t][2] = 2;
                        roommate_print.value_weights[t][3] = 2;
                    }
                }

                // Set the value_action array
                for (int v=0; v<4; v++){
                    // Add 0.5 to a randomly selected activity for four times.
                    // In this way, each activity's expected utility is 0.5.
                    for (int tt = 0; tt < 4; tt++){
                        double rand_select = random.nextDouble();
                        if (rand_select > threshold_1_outof_4){
                            roommate_print.action_value[t][v][0] += 0.5;
                        } else if (rand_select > threshold_2_outof_4) {
                            roommate_print.action_value[t][v][1] += 0.5;
                        } else if (rand_select > threshold_3_outof_4){
                            roommate_print.action_value[t][v][2] += 0.5;
                        } else {
                            roommate_print.action_value[t][v][3] += 0.5;
                        }
                    }
                }
            }

            // Initialize the utility array:
            for (int n=0; n<2000; n++){
                roommate_print.utility[n] = 0;
                roommate_print.utility_primary[n] = 0;
                roommate_print.utility_conservative[n] = 0;
            }
        }

        //Each agent keeps lists of their roommates
        Roommate_print temp;
        int i = 0;
        for(i=0; i<numRoommates; i++){
            Roommate_print roommate_print = (Roommate_print) allRoommates.get(i);
            for(int j=0; j< allRoommates.size();j++){
                if (i==j) continue;
                temp = (Roommate_print) allRoommates.get(j);

                //keep references to members in my circles.
                if (roommate_print.dormNum==temp.dormNum)
                    roommate_print.myDorm.add(temp);
            }
            schedule.scheduleRepeating(roommate_print, 0, 1.0);
        }
    }

    public void finish(){
        super.finish();

        // Extract the utility for all agents:
        for(int i=0; i<numRoommates; i++){
            Roommate_print roommate_print = (Roommate_print) allRoommates.get(i);
            System.arraycopy(roommate_print.utility, 0, utility_record[i], 0, 2000);
            System.arraycopy(roommate_print.utility_primary, 0, utility_record_primary[i], 0, 2000);
            System.arraycopy(roommate_print.utility_conservative, 0, utility_record_conservative[i], 0, 2000);
        }

        // A test output below:
        Roommate_print roommate_print0 = (Roommate_print) allRoommates.get(0);
        Roommate_print roommate_print1 = (Roommate_print) allRoommates.get(1);
        System.out.println(roommate_print0.dormNum);
        System.out.println(roommate_print1.dormNum);
        System.out.println(roommate_print0.utility[0] + roommate_print1.utility[0]);
        System.out.println(roommate_print0.utility[1] + roommate_print1.utility[1]);
        System.out.println(roommate_print0.utility[2] + roommate_print1.utility[2]);
        System.out.println(roommate_print0.utility[3] + roommate_print1.utility[3]);
        System.out.println(roommate_print0.utility[4] + roommate_print1.utility[4]);
        System.out.println(roommate_print0.utility[5] + roommate_print1.utility[5]);
        System.out.println(roommate_print0.utility[6] + roommate_print1.utility[6]);
        System.out.println(roommate_print0.utility[7] + roommate_print1.utility[7]);
        System.out.println(roommate_print0.utility[8] + roommate_print1.utility[8]);
        System.out.println(roommate_print0.utility[9] + roommate_print1.utility[9]);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("utility_record.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        writer.println(Arrays.deepToString(utility_record));
        writer.close();

        try {
            writer = new PrintWriter("utility_record_primary.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        writer.println(Arrays.deepToString(utility_record_primary));
        writer.close();

        try {
            writer = new PrintWriter("utility_record_conservative.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        writer.println(Arrays.deepToString(utility_record_conservative));
        writer.close();

        try {
            writer = new PrintWriter("utility_distribution.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        writer.println(Arrays.toString(utility_distri));
        writer.close();
    }

    public static void main(String[] args)
    {
        doLoop(Roommates_print.class, args);
        System.exit(0);
    }
}
