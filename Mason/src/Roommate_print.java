import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

public class Roommate_print implements Steppable
{
    public int dormNum = -1; // The number of this agent's dorm.
    public Bag myDorm = new Bag(); // Stores all roommate agents in this agent's dorm

    public double[][][] action_value = new double[5][4][4]; // 5 time points; 4 values; 4 alternative actions.
    // initialize the action_value array:

    public double[][] value_weights = new double[5][4]; // 5 time points; 4 values.

    public int norm_TimePoint = -1; // The time point for this agent's norm

    public int norm_Activity = -1; // The activity for this agent's norm

    public double[] utility = new double[2000]; // Utility record for ELESSAR strategy
    public double[] utility_primary = new double[2000]; // Utility record for primary strategy
    public double[] utility_conservative = new double[2000]; // Utility record for conservative strategy
    public int current_num = 0;

    public void step(SimState state) {
        Roommates_print roommates_print = (Roommates_print) state;
        double rand_num = roommates_print.random.nextDouble();
        int time_point = -1;

        // Define Threshold for later usage;
        double threshold_1_outof_4 = 0.75;
        double threshold_2_outof_4 = 0.5;
        double threshold_3_outof_4 = 0.25;

        double threshold_1_outof_5 = 0.8;
        double threshold_2_outof_5 = 0.6;
        double threshold_3_outof_5 = 0.4;
        double threshold_4_outof_5 = 0.2;

        // Random select a time point
        if (rand_num > threshold_1_outof_5){
            time_point = 0;
        } else if (rand_num > threshold_2_outof_5) {
            time_point = 1;
        } else if (rand_num > threshold_3_outof_5) {
            time_point = 2;
        } else if (rand_num > threshold_4_outof_5){
            time_point = 3;
        } else {
            time_point = 4;
        }

        // Find this agent's roommate
        Bag dorm = this.myDorm;
        Roommate_print agent_roommate = (Roommate_print) dorm.get(0);
        roommates_print.temp_num_for_output++;

        // For each value, compute the maximum and minimum utility.
        // (If the norm of this agent or its roommate is conflicted, the utility will be subtracted by 0.5.)
        double[] me_max = new double[4];
        double[] me_min = new double[4];
        double[] other_max = new double[4];
        double[] other_min = new double[4];

        // Record the S, R, and Q values for decision-making:
        double[] S_value = new double[4];
        double[] R_value = new double[4];
        double[] Q_value = new double[4];

        // For each value, find its maximum and minimum utility for this agent and its roommate
        for (int v=0; v<4; v++){
            // Initialize the maximum and minimum utility for value v:
            me_max[v] = 0;
            me_min[v] = 0.5;
            other_max[v] = 0;
            other_min[v] = 0.5;

            // For each alternative activity, update the maximum and minimum utility for value v:
            for (int a=0; a<4; a++){
                double adjust = 0; // Adjust for the activity that is prohibited by norms
                if ((this.norm_TimePoint == time_point && this.norm_Activity == a) ||
                        (agent_roommate.norm_TimePoint == time_point && agent_roommate.norm_Activity == a)){
                    adjust = -0.5;
                }
                if (this.action_value[time_point][v][a] + adjust > me_max[v]){
                    me_max[v] = this.action_value[time_point][v][a] + adjust;
                }
                if (agent_roommate.action_value[time_point][v][a] + adjust > other_max[v]){
                    other_max[v] = agent_roommate.action_value[time_point][v][a] + adjust;
                }
                if (this.action_value[time_point][v][a] + adjust < me_min[v]){
                    me_min[v] = this.action_value[time_point][v][a] + adjust;
                }
                if (agent_roommate.action_value[time_point][v][a] + adjust < other_min[v]){
                    other_min[v] = agent_roommate.action_value[time_point][v][a] + adjust;
                }
            }
        }

        // Maximum and minimum value of S and R. They are initialized to be the first activity.
        int S_max = 0;
        int S_min = 0;
        int R_max = 0;
        int R_min = 0;

        for (int a=0; a<4; a++){
            // Initialize the S,R and Q values:
            S_value[a] = 0;
            R_value[a] = 0;
            Q_value[a] = 0;

            double adjust = 0; // Adjust for the activity that is prohibited by norms
            if ((this.norm_TimePoint == time_point && this.norm_Activity == a) ||
                    (agent_roommate.norm_TimePoint == time_point && agent_roommate.norm_Activity == a)){
                adjust = -0.5;
            }

            double temp_weighted; // A temporary number for convenience: it represents the weighted utility;

            // For each activity, compute the weighted and normalized Manhattan distance and Chebyshev distance
            for (int v=0; v<4; v++){
                if (me_max[v] - me_min[v] != 0){
                    temp_weighted = this.value_weights[time_point][v] * (me_max[v] - this.action_value[time_point][v][a] - adjust)/(me_max[v] - me_min[v]);
                    S_value[a] += temp_weighted;
                    if (temp_weighted > R_value[a]){
                        R_value[a] = temp_weighted;
                    }
                }
                if (other_max[v] - other_min[v] != 0){
                    temp_weighted = agent_roommate.value_weights[time_point][v] * (other_max[v] - agent_roommate.action_value[time_point][v][a] - adjust)/
                            (other_max[v] - other_min[v]);
                    S_value[a] += temp_weighted;
                    if (temp_weighted > R_value[a]){
                        R_value[a] = temp_weighted;
                    }
                }
            }

            // Update the maximum and minimum values of S and R:
            if (a != 0){
                if (S_value[a] > S_value[S_max]){
                    S_max = a;
                }
                if (S_value[a] < S_value[S_min]){
                    S_min = a;
                }
                if (R_value[a] > R_value[R_max]){
                    R_max = a;
                }
                if (R_value[a] < R_value[R_min]){
                    R_min = a;
                }
            }
        }

        // Record the activity that minimizes the Q value:
        int Q_min = 0;

        // For each activity, compute the Q which combines the Manhattan distance and Chebyshev distance
        for (int a=0; a<4; a++){
            if (S_value[S_max] - S_value[S_min] != 0){
                Q_value[a] += 0.5 * (S_value[a] - S_value[S_min])/(S_value[S_max] - S_value[S_min]);
            }
            if (R_value[R_max] - R_value[R_min] != 0){
                Q_value[a] += 0.5 * (R_value[a] - R_value[S_min])/(R_value[R_max] - R_value[R_min]);
            }
            if (Q_value[a] < Q_value[Q_min]){
                Q_min = a;
            }
        }

        // Record the activity that maximizes the primary agent's utility
        int a_primary = 0;
        double max_value_sum = 0;

        for (int a=0; a<4; a++){
            double value_sum = 0;
            double adjust = 0.0; // Adjust for the activity that is prohibited by norms
            if ((this.norm_TimePoint == time_point && this.norm_Activity == a) ||
                    (agent_roommate.norm_TimePoint == time_point && agent_roommate.norm_Activity == a)){
                adjust = -0.5;
            }
            for (int v=0; v<4; v++){
                value_sum = value_sum + (this.action_value[time_point][v][a] + adjust) * this.value_weights[time_point][v];
            }
            if (roommates_print.distri_num < 10000){
                roommates_print.utility_distri[roommates_print.distri_num] = value_sum;
                roommates_print.distri_num ++;
            }
            if (value_sum > max_value_sum){
                max_value_sum = value_sum;
                a_primary = a;
            }
        }

        // Record the activity for the conservative decision-making strategy.
        int a_conservative = 0;
        if (time_point < 4){
            a_conservative = time_point;
        }

        // Record the utility for the agent and its roommate:
        double adjust = 0.0; // Adjust for the activity that is prohibited by norms
        if ((this.norm_TimePoint == time_point && this.norm_Activity == Q_min) ||
                (agent_roommate.norm_TimePoint == time_point && agent_roommate.norm_Activity == Q_min)){
            adjust = -0.5;
        }
        double adjust_primary = 0.0; // Adjust for the activity that is prohibited by norms
        if ((this.norm_TimePoint == time_point && this.norm_Activity == a_primary) ||
                (agent_roommate.norm_TimePoint == time_point && agent_roommate.norm_Activity == a_primary)){
            adjust_primary = -0.5;
        }
        double adjust_conservative = 0.0; // Adjust for the activity that is prohibited by norms
        if ((this.norm_TimePoint == time_point && this.norm_Activity == a_conservative) ||
                (agent_roommate.norm_TimePoint == time_point && agent_roommate.norm_Activity == a_conservative)){
            adjust_conservative = -0.5;
        }
        for (int v=0; v<4; v++){
            this.utility[this.current_num] += this.value_weights[time_point][v] * (this.action_value[time_point][v][Q_min] + adjust);
            agent_roommate.utility[agent_roommate.current_num] += agent_roommate.value_weights[time_point][v] *
                    (agent_roommate.action_value[time_point][v][Q_min] + adjust);
            this.utility_primary[this.current_num] += this.value_weights[time_point][v] * (this.action_value[time_point][v][a_primary] + adjust_primary);
            agent_roommate.utility_primary[agent_roommate.current_num] += agent_roommate.value_weights[time_point][v] *
                    (agent_roommate.action_value[time_point][v][a_primary] + adjust_primary);
            this.utility_conservative[this.current_num] += this.value_weights[time_point][v] * (this.action_value[time_point][v][a_conservative] + adjust_conservative);
            agent_roommate.utility_conservative[agent_roommate.current_num] += agent_roommate.value_weights[time_point][v] *
                    (agent_roommate.action_value[time_point][v][a_conservative] + adjust_conservative);
        }
        this.current_num ++;
        agent_roommate.current_num ++;

        // Change the norm for the agent
        // 1. Randomly select a time point for this agent's norm:
        double rand_num_norm = roommates_print.random.nextDouble();
        if (rand_num_norm > threshold_1_outof_5){
            this.norm_TimePoint = 0;
        } else if (rand_num_norm > threshold_2_outof_5) {
            this.norm_TimePoint = 1;
        } else if (rand_num_norm > threshold_3_outof_5) {
            this.norm_TimePoint = 2;
        } else if (rand_num_norm > threshold_4_outof_5) {
            this.norm_TimePoint = 3;
        } else {
            this.norm_TimePoint = 4;
        }

        // 2. Randomly select an activity for this agent's norm:
        rand_num_norm = roommates_print.random.nextDouble();
        if (rand_num_norm > threshold_1_outof_4){
            this.norm_Activity = 0;
        } else if (rand_num_norm > threshold_2_outof_4) {
            this.norm_Activity = 1;
        } else if (rand_num_norm > threshold_3_outof_4) {
            this.norm_Activity = 2;
        } else {
            this.norm_Activity = 3;
        }
    }
}